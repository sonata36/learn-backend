package com.learn.learnbackend.app;

import com.learn.learnbackend.model.OrderSort;
import com.learn.learnbackend.model.CustomerOrder;
import com.learn.learnbackend.model.ProductSort;
import com.learn.learnbackend.service.OrderService;
import com.learn.learnbackend.service.ProductService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/** 简单交互式控制台。设置 app.console.enabled=false 可关闭，便于测试或后续接 Web 接口。 */
@Component
@ConditionalOnProperty(name = "app.console.enabled", havingValue = "true", matchIfMissing = true)
public class ConsoleMenu implements CommandLineRunner {
    private final ProductService products;
    private final OrderService orders;
    public ConsoleMenu(ProductService products, OrderService orders) { this.products = products; this.orders = orders; }

    @Override
    public void run(String... args) {
        try (Scanner input = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n=== 工作室订单记账 ===");
                System.out.println("1 商品列表  2 新增商品  3 修改商品  4 停用商品");
                System.out.println("5 创建订单  6 订单列表  7 订单详情  8 修改订单商品  9 标记删除订单  0 退出");
                System.out.print("请选择: ");
                String command = input.nextLine().trim();
                if (command.equals("0")) return;
                try { dispatch(command, input); }
                catch (RuntimeException e) {
                    String message = e.getMessage();
                    System.out.println("操作失败: " + (message == null || message.isBlank()
                            ? "输入内容无效，请检查商品名称、编号、价格和数量后重试"
                            : message));
                }
            }
        }
    }

    private void dispatch(String command, Scanner in) {
        switch (command) {
            case "1" -> products.findAll(ProductSort.ID_ASC, false).forEach(System.out::println);
            case "2" -> {
                var product = products.create(ask(in, "商品名称: "), readPrice(in, "价格: "));
                System.out.println("商品新增成功，商品编号：" + product.id());
            }
            case "3" -> {
                products.update(longValue(ask(in, "商品编号: ")), ask(in, "新名称: "), readPrice(in, "新价格: "));
                System.out.println("商品修改成功");
            }
            case "4" -> {
                products.delete(longValue(ask(in, "要停用的商品编号: ")));
                System.out.println("商品停用成功");
            }
            case "5" -> {
                var order = orders.create(readQuantities(in));
                System.out.println("订单创建成功，订单编号：" + order.id() + "，总价：" + order.totalPrice());
            }
            case "6" -> {
                var orderList = orders.findAll(orderSort(ask(in, "排序 idAsc/timeAsc/timeDesc/priceAsc/priceDesc: ")));
                if (orderList.isEmpty()) System.out.println("暂无订单");
                else orderList.forEach(this::printOrderSummary);
            }
            case "7" -> {
                var order = orders.findById(longValue(ask(in, "订单编号: ")))
                        .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
                printOrderDetails(order);
            }
            case "8" -> {
                var order = orders.update(longValue(ask(in, "订单编号: ")), readQuantities(in));
                System.out.println("订单修改成功，订单编号：" + order.id() + "，新总价：" + order.totalPrice());
            }
            case "9" -> {
                orders.delete(longValue(ask(in, "要删除的订单编号: ")));
                System.out.println("订单已标记为删除，历史明细仍可查询");
            }
            default -> System.out.println("未知选项，请输入 0 至 9 之间的菜单编号");
        }
    }

    private Map<Long, Integer> readQuantities(Scanner in) {
        System.out.println("请输入商品编号:数量，以逗号分隔，例如 1:2,3:1");
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (String pair : ask(in, "商品及数量: ").split(",")) {
            String[] parts = pair.trim().split(":");
            if (parts.length != 2) throw new IllegalArgumentException("格式应为 商品编号:数量");
            if (result.put(longValue(parts[0]), integerValue(parts[1], "商品数量")) != null) throw new IllegalArgumentException("同一商品请只输入一次");
        }
        return result;
    }

    private OrderSort orderSort(String value) {
        return switch (value.trim().toLowerCase()) {
            case "idasc" -> OrderSort.ID_ASC;
            case "timeasc" -> OrderSort.TIME_ASC; case "timedesc" -> OrderSort.TIME_DESC;
            case "priceasc" -> OrderSort.PRICE_ASC; case "pricedesc" -> OrderSort.PRICE_DESC;
            default -> throw new IllegalArgumentException("排序值无效");
        };
    }

    /** 以简短的一行格式显示订单列表摘要。 */
    private void printOrderSummary(CustomerOrder order) {
        System.out.printf("订单编号：%d | 下单时间：%s | 总价：%s | 状态：%s%n",
                order.id(), order.orderedAt(), order.totalPrice(), order.deletedAt() == null ? "有效" : "已删除");
    }

    /** 分行显示订单头和商品明细，避免 record 的默认字符串过长。 */
    private void printOrderDetails(CustomerOrder order) {
        System.out.println("订单详情");
        System.out.println("订单编号：" + order.id());
        System.out.println("下单时间：" + order.orderedAt());
        System.out.println("订单总价：" + order.totalPrice());
        System.out.println("状态：" + (order.deletedAt() == null ? "有效" : "已删除"));
        if (order.deletedAt() != null) System.out.println("删除时间：" + order.deletedAt());
        System.out.println("商品明细：");
        if (order.items().isEmpty()) {
            System.out.println("  （无商品明细）");
            return;
        }
        for (var item : order.items()) {
            System.out.printf("  商品：%s（编号：%d）%n", item.productName(), item.productId());
            System.out.printf("    单价：%s | 数量：%d | 小计：%s%n",
                    item.unitPrice(), item.quantity(), item.subtotal());
        }
    }

    private String ask(Scanner in, String prompt) { System.out.print(prompt); return in.nextLine().trim(); }

    /** 读取并验证价格，避免 NumberFormatException 输出空信息。 */
    private BigDecimal readPrice(Scanner in, String prompt) {
        String value = ask(in, prompt);
        if (value.isBlank()) throw new IllegalArgumentException("价格不能为空，请输入例如 39.90 的金额");
        BigDecimal price;
        try { price = new BigDecimal(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("价格格式不正确，请输入数字，例如 39.90"); }
        if (price.signum() < 0) throw new IllegalArgumentException("价格不能为负数");
        if (price.scale() > 2) throw new IllegalArgumentException("价格最多保留两位小数");
        if (price.compareTo(new BigDecimal("9999999999.99")) > 0) throw new IllegalArgumentException("价格过大，不能超过 9999999999.99");
        return price;
    }

    /** 将编号转换为正整数；为格式错误提供面向用户的提示。 */
    private long longValue(String value) {
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) throw new IllegalArgumentException("编号必须是正整数");
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("编号格式不正确，请输入正整数");
        }
    }

    /** 将数量文本转换为整数；正数范围由订单 Service 继续校验。 */
    private int integerValue(String value, String fieldName) {
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(fieldName + "格式不正确，请输入整数"); }
    }
}
