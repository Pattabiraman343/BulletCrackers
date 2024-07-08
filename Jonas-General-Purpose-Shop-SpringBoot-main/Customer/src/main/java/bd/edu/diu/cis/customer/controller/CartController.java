package bd.edu.diu.cis.customer.controller;

import bd.edu.diu.cis.library.model.Customer;
import bd.edu.diu.cis.library.model.Product;
import bd.edu.diu.cis.library.model.ShoppingCart;
import bd.edu.diu.cis.library.service.CustomerService;
import bd.edu.diu.cis.library.service.ProductService;
import bd.edu.diu.cis.library.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;

@Controller
public class CartController {
    @Autowired
    private CustomerService customerService;

    @Autowired
    private ShoppingCartService cartService;

    @Autowired
    private ProductService productService;

    @GetMapping("/cart")
    public String cart(Model model, Principal principal, HttpSession session) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        Customer customer = customerService.findByUsername(username);

        if (customer == null || customer.getShoppingCart() == null) {
            // Handle scenario where customer or shopping cart is null
            return "redirect:/products"; // Or redirect to another page as needed
        }

        ShoppingCart shoppingCart = customer.getShoppingCart();

        session.setAttribute("totalItems", shoppingCart.getTotalItems());
        model.addAttribute("subTotal", shoppingCart.getTotalPrices());
        model.addAttribute("discountPrice", customerService.calculateDiscount(shoppingCart.getTotalPrices()));
        model.addAttribute("grandTotal", shoppingCart.getTotalPrices()); // Ensure grand total is added

        model.addAttribute("shoppingCart", shoppingCart);

        return "shoping-cart";
    }



    @PostMapping("/add-to-cart")
    public String addItemToCart(
            @RequestParam("id") Long productId,
            @RequestParam("quantity") Integer quantity,            Principal principal,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes){

        if(principal == null){
            return "redirect:/login";
        }
        Product product = productService.getProductById(productId);
        String username = principal.getName();
        Customer customer = customerService.findByUsername(username);

        if (product.getCurrentQuantity() <= 0) {
            redirectAttributes.addFlashAttribute("outOfStockMessage", "Sorry, '" + product.getName() + "' is out of stock.");
            return "redirect:" + request.getHeader("Referer");
        }

        ShoppingCart cart = cartService.addItemToCart(product, quantity, customer);
        return "redirect:" + request.getHeader("Referer");
    }


    @RequestMapping(value = "/update-cart", method = RequestMethod.POST, params = "action=update")
    public String updateCart(@RequestParam("quantity") int quantity,
                             @RequestParam("id") Long productId,
                             Model model,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        } else {
            String username = principal.getName();
            Customer customer = customerService.findByUsername(username);
            Product product = productService.getProductById(productId);

            // Check if requested quantity exceeds available stock
            if (quantity > product.getCurrentQuantity()) {
                redirectAttributes.addFlashAttribute("insufficientStock", "Requested quantity exceeds available stock for " + product.getName() + ". Available stock: " + product.getCurrentQuantity());
                return "redirect:/cart";
            }

            ShoppingCart cart = cartService.updateItemInCart(product, quantity, customer);

            model.addAttribute("shoppingCart", cart);
            return "redirect:/cart";
        }
    }



    @RequestMapping(value = "/update-cart", method = RequestMethod.POST, params = "action=delete")
    public String deleteItemFromCart(@RequestParam("id") Long productId,
                                     Model model,
                                     Principal principal){
        if(principal == null){
            return "redirect:/login";
        }else{
            String username = principal.getName();
            Customer customer = customerService.findByUsername(username);
            Product product = productService.getProductById(productId);
            ShoppingCart cart = cartService.deleteItemFromCart(product, customer);
            model.addAttribute("shoppingCart", cart);
            return "redirect:/cart";
        }

    }
}
