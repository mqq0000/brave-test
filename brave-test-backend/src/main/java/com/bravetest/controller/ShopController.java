package com.bravetest.controller;

import com.bravetest.common.R;
import com.bravetest.common.UserContext;
import com.bravetest.entity.ShopItem;
import com.bravetest.entity.TradeOrder;
import com.bravetest.mapper.TradeOrderMapper;
import com.bravetest.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "04-冒险者商城")
@RestController
@RequestMapping("/api/v1/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final TradeOrderMapper tradeOrderMapper;

    @Operation(summary = "商品列表（itemType: 1普通 2净化剂）")
    @GetMapping("/items")
    public R<List<ShopItem>> items(@RequestParam(required = false) Integer itemType,
                                   @RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "10") long size) {
        return R.ok(shopService.listItems(itemType, page, size));
    }

    @Operation(summary = "冒险者上架自己的商品")
    @PostMapping("/items")
    public R<Long> listItem(@RequestParam String name,
                            @RequestParam Long price,
                            @RequestParam Integer stock) {
        return R.ok(shopService.listItem(UserContext.getUserId(), name, price, stock));
    }

    @Operation(summary = "购买商品（净化剂支付5万金币/支）")
    @PostMapping("/orders/{itemId}")
    public R<Long> purchase(@PathVariable Long itemId) {
        return R.ok(shopService.purchase(UserContext.getUserId(), itemId));
    }

    @Operation(summary = "我的订单")
    @GetMapping("/orders/mine")
    public R<List<TradeOrder>> myOrders() {
        return R.ok(tradeOrderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getBuyerId, UserContext.getUserId())
                        .orderByDesc(TradeOrder::getCreatedAt)));
    }

    @Operation(summary = "使用净化剂（污染值-50）")
    @PostMapping("/purifier/use")
    public R<Void> usePurifier() {
        shopService.usePurifier(UserContext.getUserId());
        return R.ok();
    }
}
