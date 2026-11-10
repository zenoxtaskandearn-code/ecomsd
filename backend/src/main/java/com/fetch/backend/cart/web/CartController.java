package com.fetch.backend.cart.web;

import com.fetch.backend.cart.service.CartService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;
    public CartController(CartService cartService) { this.cartService = cartService; }

    public record AddItemRequest(@NotNull Long variantId, @Min(1) int quantity) {}
    public record UpdateItemRequest(@Min(0) int quantity) {}

    @GetMapping public Map<String, Object> get() { return cartService.getCart(); }
    @PostMapping("/items") public Map<String, Object> add(@RequestBody AddItemRequest req) { return cartService.addItem(req.variantId(), req.quantity()); }
    @PatchMapping("/items/{itemId}") public Map<String, Object> update(@PathVariable Long itemId, @RequestBody UpdateItemRequest req) { return cartService.updateItem(itemId, req.quantity()); }
    @DeleteMapping("/items/{itemId}") public Map<String, Object> remove(@PathVariable Long itemId) { return cartService.removeItem(itemId); }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) public void clear() { cartService.clear(); }
}
