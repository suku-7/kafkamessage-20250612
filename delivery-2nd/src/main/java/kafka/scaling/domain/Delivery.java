package kafka.scaling.domain;

import java.util.Date;
import java.util.List;
import javax.persistence.*;
import kafka.scaling.DeliveryApplication;
import lombok.Data;

@Entity
@Table(name = "Delivery_table")
@Data
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long orderId;

    private Long customerId;

    private String productId;

    private String productName;

    private Integer qty;

    private String address;

    private String status;

    public static DeliveryRepository repository() {
        DeliveryRepository deliveryRepository = DeliveryApplication.applicationContext.getBean(
            DeliveryRepository.class
        );
        return deliveryRepository;
    }

    public static void startDelivery(OrderPlaced orderPlaced) {
        // Example 1:  new item 
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderPlaced.getId());
        delivery.setProductId(orderPlaced.getProductId());
        delivery.setProductName(orderPlaced.getProductName());
        delivery.setCustomerId(orderPlaced.getCustomerId());
        delivery.setQty(orderPlaced.getQty());
        delivery.setStatus("DELIVERY STARTED");

        repository().save(delivery);

        DeliveryStarted deliveryStarted = new DeliveryStarted(delivery);
        deliveryStarted.publishAfterCommit();

        /** Example 2:  finding and process
        
        repository().findById(orderPlaced.get???()).ifPresent(delivery->{
            
            delivery // do something
            repository().save(delivery);

            DeliveryStarted deliveryStarted = new DeliveryStarted(delivery);
            deliveryStarted.publishAfterCommit();

         });
        */

    }

    public static void modifyDelivery(OrderModified orderModified) {
        /** Example 1:  new item 
        Delivery delivery = new Delivery();
        repository().save(delivery);

        DeliveryModified deliveryModified = new DeliveryModified(delivery);
        deliveryModified.publishAfterCommit();
        */

        /** Example 2:  finding and process */
        repository().findById(orderModified.getId()).ifPresentOrElse(delivery ->{  
            
            delivery.setAddress(orderModified.getAddress()); // do something
            repository().save(delivery);

            DeliveryModified deliveryModified = new DeliveryModified(delivery);
            deliveryModified.publishAfterCommit();

         }
         ,()->{
             throw new RuntimeException("수정할 대상 정보를 찾을 수 없습니다.");
         }
        );
    }

    public static void cancelDelivery(OrderCancelled orderCancelled) {
        /** Example 1:  new item 
        Delivery delivery = new Delivery();
        repository().save(delivery);

        DeliveryCancelled deliveryCancelled = new DeliveryCancelled(delivery);
        deliveryCancelled.publishAfterCommit();
        */

        /** Example 2:  finding and process */
        
        repository().findById(orderCancelled.getId()).ifPresentOrElse(delivery ->{  
            
            // do something
            repository().delete(delivery);

            DeliveryModified deliveryModified = new DeliveryModified(delivery);
            deliveryModified.publishAfterCommit();

        }
        ,()->{
            throw new RuntimeException("삭제할 대상 정보를 찾을 수 없습니다.");
        }
       );
   }
}
