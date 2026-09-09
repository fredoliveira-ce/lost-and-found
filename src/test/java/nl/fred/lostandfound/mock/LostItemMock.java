package nl.fred.lostandfound.mock;

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

}
