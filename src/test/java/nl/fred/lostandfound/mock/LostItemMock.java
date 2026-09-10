package nl.fred.lostandfound.mock;

import java.time.Instant;
import nl.fred.lostandfound.domain.entity.LostItem;

public class LostItemMock {

  public static LostItem getOne() {
    return getOneWithQuantity(2);
  }

  public static LostItem getOneWithQuantity(int quantity) {
    return LostItem.builder()
        .itemName("Laptop")
        .quantity(quantity)
        .place("Airport")
        .build();
  }

  public static LostItem getOne(String itemName, String place) {
    return LostItem.builder()
        .itemName(itemName)
        .quantity(2)
        .place(place)
        .build();
  }

  public static LostItem getOneWithCreatedAt(String itemName, String place, Instant createdAt) {
    return LostItem.builder()
        .itemName(itemName)
        .quantity(2)
        .place(place)
        .createdAt(createdAt)
        .build();
  }

}
