package bd.edu.diu.cis.library.service.impl;

import bd.edu.diu.cis.library.repository.CartItemRepository;
import bd.edu.diu.cis.library.repository.ProductRepository;
import bd.edu.diu.cis.library.repository.ShoppingCartRepository;
import bd.edu.diu.cis.library.model.CartItem;
import bd.edu.diu.cis.library.model.Customer;
import bd.edu.diu.cis.library.model.Product;
import bd.edu.diu.cis.library.model.ShoppingCart;
import bd.edu.diu.cis.library.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private CartItemRepository itemRepository;

    @Autowired
    private ShoppingCartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public ShoppingCart addItemToCart(Product product, int quantity, Customer customer) {
        ShoppingCart cart = customer.getShoppingCart();

        if (cart == null) {
            cart = new ShoppingCart();
            cart.setCustomer(customer);
            cart.setCartItem(new HashSet<>()); // Initialize cart items set
        }

        Set<CartItem> cartItems = cart.getCartItem();
        CartItem cartItem = findCartItem(cartItems, product.getId());

        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setTotalPrice(quantity * product.getSalePrice());
            cartItem.setCart(cart);
            cartItems.add(cartItem);
        } else {
            int newQuantity = cartItem.getQuantity() + quantity;
            cartItem.setQuantity(newQuantity);
            cartItem.setTotalPrice(newQuantity * product.getSalePrice());
        }

        cart.setCartItem(cartItems);
        updateProductStock(product, -quantity); // Decrease product stock

        // Update cart totals immediately
        updateCartTotals(cart);

        // Save the updated cart
        return cartRepository.save(cart);
    }

    private void updateCartTotals(ShoppingCart cart) {
        Set<CartItem> cartItems = cart.getCartItem();
        double totalPrice = totalPrice(cartItems);
        int totalItems = totalItems(cartItems);

        cart.setTotalItems(totalItems);
        cart.setTotalPrices(totalPrice);
    }

    @Override
    public ShoppingCart updateItemInCart(Product product, int quantity, Customer customer) {
        ShoppingCart cart = customer.getShoppingCart();
        Set<CartItem> cartItems = cart.getCartItem();
        CartItem cartItem = findCartItem(cartItems, product.getId());

        if (cartItem != null) {
            int oldQuantity = cartItem.getQuantity();
            int diffQuantity = quantity - oldQuantity;
            cartItem.setQuantity(quantity);
            cartItem.setTotalPrice(quantity * product.getSalePrice());

            cart.setCartItem(cartItems);
            updateProductStock(product, -diffQuantity); // Adjust product stock
        }

        double totalPrice = totalPrice(cartItems);
        int totalItems = totalItems(cartItems);

        cart.setTotalItems(totalItems);
        cart.setTotalPrices(totalPrice);

        // Save the updated cart
        cart = cartRepository.save(cart);

        return cart;
    }

    @Override
    public ShoppingCart deleteItemFromCart(Product product, Customer customer) {
        ShoppingCart cart = customer.getShoppingCart();
        Set<CartItem> cartItems = cart.getCartItem();
        CartItem cartItem = findCartItem(cartItems, product.getId());

        if (cartItem != null) {
            cartItems.remove(cartItem);
            cart.setCartItem(cartItems);
            updateProductStock(product, cartItem.getQuantity()); // Increment product stock
        }

        itemRepository.delete(cartItem); // Delete cart item from repository

        double totalPrice = totalPrice(cartItems);
        int totalItems = totalItems(cartItems);

        cart.setTotalItems(totalItems);
        cart.setTotalPrices(totalPrice);

        // Save the updated cart
        cart = cartRepository.save(cart);

        return cart;
    }

    private void updateProductStock(Product product, int quantity) {
        int newStock = product.getCurrentQuantity() + quantity;
        if (newStock < 0) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
        }
        product.setCurrentQuantity(newStock);
        productRepository.save(product);
    }


    private CartItem findCartItem(Set<CartItem> cartItems, Long productId) {
        if (cartItems == null) {
            return null;
        }
        return cartItems.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private int totalItems(Set<CartItem> cartItems){
        int totalItems = 0;
        for(CartItem item : cartItems){
            totalItems += item.getQuantity();
        }
        return totalItems;
    }

    private double totalPrice(Set<CartItem> cartItems){
        double totalPrice = 0.0;
        for(CartItem item : cartItems){
            totalPrice += item.getTotalPrice();
        }
        return totalPrice;
    }
}
