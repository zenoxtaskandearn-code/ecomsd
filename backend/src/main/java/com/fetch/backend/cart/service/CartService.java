package com.fetch.backend.cart.service;

import com.fetch.backend.cart.entity.Cart;
import com.fetch.backend.cart.entity.CartItem;
import com.fetch.backend.cart.repo.CartItemRepository;
import com.fetch.backend.cart.repo.CartRepository;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.ProductImageRepository;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final UserService userService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductVariantRepository variantRepository,
                       ProductImageRepository imageRepository, UserService userService) {
        this.cartRepository = cartRepository; this.cartItemRepository = cartItemRepository; this.variantRepository = variantRepository;
        this.imageRepository = imageRepository; this.userService = userService;
    }

    @Transactional
    public Map<String, Object> getCart() { return toResponse(getOrCreateActiveCart()); }

    @Transactional
    public Map<String, Object> addItem(Long variantId, int quantity) {
        if (quantity < 1) throw new BadRequestException("Quantity must be at least 1");
        Cart cart = getOrCreateActiveCart();
        ProductVariant v = variantRepository.findById(variantId).orElseThrow(() -> new NotFoundException("Variant not found"));
        if (!v.isActive() || !"ACTIVE".equals(v.getProduct().getStatus())) throw new BadRequestException("Item is unavailable");
        CartItem item = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variantId).orElseGet(() -> {
            CartItem ci = new CartItem(); ci.setCartId(cart.getId()); ci.setProductVariantId(variantId); ci.setQuantity(0); return ci;
        });
        item.setQuantity(item.getQuantity() + quantity);
        item.setUnitPriceSnapshot(v.getPriceAmount());
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public Map<String, Object> updateItem(Long itemId, int quantity) {
        Cart cart = getOrCreateActiveCart();
        CartItem item = cartItemRepository.findById(itemId).filter(i -> i.getCartId().equals(cart.getId())).orElseThrow(() -> new NotFoundException("Cart item not found"));
        if (quantity <= 0) { cartItemRepository.delete(item); }
        else { item.setQuantity(quantity); cartItemRepository.save(item); }
        return toResponse(cart);
    }

    @Transactional
    public Map<String, Object> removeItem(Long itemId) { return updateItem(itemId, 0); }

    @Transactional
    public void clear() { Cart cart = getOrCreateActiveCart(); cartItemRepository.deleteByCartId(cart.getId()); }

    private Cart getOrCreateActiveCart() {
        Long userId = userService.currentUser().getId();
        return cartRepository.findByUserIdAndStatus(userId, "ACTIVE").orElseGet(() -> {
            Cart c = new Cart(); c.setUserId(userId); c.setStatus("ACTIVE"); c.setCurrency("USD"); return cartRepository.save(c);
        });
    }

    private Map<String, Object> toResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        BigDecimal subtotal = BigDecimal.ZERO;
        List<Map<String, Object>> out = new ArrayList<>();
        for (CartItem i : items) {
            ProductVariant v = variantRepository.findById(i.getProductVariantId()).orElse(null);
            if (v == null) continue;
            BigDecimal line = v.getPriceAmount().multiply(BigDecimal.valueOf(i.getQuantity()));
            subtotal = subtotal.add(line);
            String image = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(v.getProduct().getId()).stream().findFirst().map(im -> im.getUrl()).orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i.getId());
            row.put("variantId", v.getId());
            row.put("slug", v.getProduct().getSlug());
            row.put("name", v.getProduct().getName());
            row.put("variant", v.getTitle());
            row.put("image", image);
            row.put("quantity", i.getQuantity());
            row.put("unitPrice", v.getPriceAmount());
            row.put("lineTotal", line);
            row.put("currency", "USD");
            out.add(row);
        }
        return Map.of("id", cart.getId(), "currency", cart.getCurrency(), "items", out, "subtotal", subtotal, "count", items.stream().mapToInt(CartItem::getQuantity).sum());
    }
}
