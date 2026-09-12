package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.PurificationBatch;
import com.bravetest.entity.ShopItem;
import com.bravetest.entity.TradeOrder;
import com.bravetest.mapper.PurificationBatchMapper;
import com.bravetest.mapper.ShopItemMapper;
import com.bravetest.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 冒险者商城服务：普通商品交易 / 净化剂购买与使用
 * <p>订单状态约定：普通商品 1=已完成；净化剂 1=已购未使用 3=已使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    public static final int ITEM_NORMAL = 1;
    public static final int ITEM_PURIFIER = 2;
    /** 净化剂统一售价 */
    public static final long PURIFIER_PRICE = 50000L;
    /** 使用净化剂的净化值 */
    public static final int PURIFIER_PURIFY_VALUE = 50;

    private final ShopItemMapper shopItemMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final PurificationBatchMapper batchMapper;
    private final com.bravetest.mapper.AdventurerMapper adventurerMapper;
    private final FinanceService financeService;
    private final AdventurerService adventurerService;

    public List<ShopItem> listItems(Integer itemType, long page, long size) {
        return shopItemMapper.selectList(new LambdaQueryWrapper<ShopItem>()
                .eq(ShopItem::getStatus, 1)
                .eq(itemType != null, ShopItem::getItemType, itemType)
                .orderByDesc(ShopItem::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size));
    }

    /**
     * 冒险者上架自己的商品交易
     */
    public Long listItem(Long userId, String name, Long price, Integer stock) {
        if (price == null || price <= 0 || stock == null || stock <= 0) {
            throw new BusinessException("价格与库存必须大于0");
        }
        ShopItem item = new ShopItem();
        item.setName(name);
        item.setItemType(ITEM_NORMAL);
        item.setPrice(price);
        item.setStock(stock);
        item.setSellerId(userId);
        item.setStatus(1);
        item.setCreatedAt(LocalDateTime.now());
        shopItemMapper.insert(item);
        return item.getId();
    }

    /**
     * 冒险者协会管理员上架净化剂（库存取当月批次剩余总量）
     */
    @Transactional
    public Long listPurifier(Long adminUserId) {
        int available = batchMapper.selectList(new LambdaQueryWrapper<PurificationBatch>()
                        .gt(PurificationBatch::getRemaining, 0))
                .stream().mapToInt(PurificationBatch::getRemaining).sum();
        if (available <= 0) {
            throw new BusinessException("暂无净化剂库存，请等待总管理员生产");
        }
        ShopItem item = new ShopItem();
        item.setName("净化剂");
        item.setItemType(ITEM_PURIFIER);
        item.setPrice(PURIFIER_PRICE);
        item.setStock(available);
        item.setSellerId(adminUserId);
        item.setStatus(1);
        item.setCreatedAt(LocalDateTime.now());
        shopItemMapper.insert(item);
        return item.getId();
    }

    /**
     * 购买商品：乐观扣库存 + 乐观扣款 + 订单 + 卖家入账/协会账本
     */
    @Transactional
    public Long purchase(Long userId, Long itemId) {
        Adventurer buyer = financeService.requireAdventurerByUserId(userId);
        ShopItem item = shopItemMapper.selectById(itemId);
        if (item == null || item.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        // 乐观扣库存
        int rows = shopItemMapper.update(null, new LambdaUpdateWrapper<ShopItem>()
                .eq(ShopItem::getId, itemId)
                .gt(ShopItem::getStock, 0)
                .setSql("stock = stock - 1"));
        if (rows == 0) {
            throw new BusinessException("库存不足");
        }
        // 乐观扣款
        financeService.deductGold(buyer.getId(), item.getPrice(), "SHOP_BUY", "SHOP_ITEM", itemId);

        TradeOrder order = new TradeOrder();
        order.setItemId(itemId);
        order.setBuyerId(userId);
        order.setSellerId(item.getSellerId());
        order.setPrice(item.getPrice());
        order.setType(item.getItemType() == ITEM_PURIFIER ? 2 : 1);
        order.setStatus(1);
        order.setCreatedAt(LocalDateTime.now());
        tradeOrderMapper.insert(order);

        if (item.getItemType() == ITEM_PURIFIER) {
            // 净化剂销售：收入入冒险者协会账本，并消耗一批次
            financeService.ledgerDelta(FinanceService.ASSOC_ADVENTURER,
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                    item.getPrice(), 0);
            PurificationBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<PurificationBatch>()
                    .gt(PurificationBatch::getRemaining, 0)
                    .orderByAsc(PurificationBatch::getProduceMonth)
                    .last("LIMIT 1"));
            if (batch != null) {
                batchMapper.update(null, new LambdaUpdateWrapper<PurificationBatch>()
                        .eq(PurificationBatch::getId, batch.getId())
                        .gt(PurificationBatch::getRemaining, 0)
                        .setSql("remaining = remaining - 1"));
            }
        } else {
            // 普通商品：卖家为冒险者则直接入账（卖家为管理员时无冒险者档案，跳过）
            Adventurer seller = adventurerMapper.selectOne(
                    new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, item.getSellerId()));
            if (seller != null) {
                financeService.addGold(seller.getId(), item.getPrice(), "SHOP_SELL", "SHOP_ITEM", itemId);
            }
        }
        return order.getId();
    }

    /**
     * 使用净化剂：消耗一支（最早购买的未使用订单），污染值 -50
     */
    @Transactional
    public void usePurifier(Long userId) {
        Adventurer adv = financeService.requireAdventurerByUserId(userId);
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getBuyerId, userId)
                .eq(TradeOrder::getType, 2)
                .eq(TradeOrder::getStatus, 1)
                .orderByAsc(TradeOrder::getCreatedAt)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("没有可使用的净化剂，请先在商城购买");
        }
        order.setStatus(3);
        tradeOrderMapper.updateById(order);
        adventurerService.purify(adv.getId(), PURIFIER_PURIFY_VALUE, "PURIFIER", order.getId());
    }
}
