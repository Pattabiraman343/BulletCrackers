package bd.edu.diu.cis.library.repository;

import bd.edu.diu.cis.library.model.Customer;
import bd.edu.diu.cis.library.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Order findById(long id);

    List<Order> findByCustomer(Customer customer);
}
