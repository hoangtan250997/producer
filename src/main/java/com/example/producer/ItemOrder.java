package com.example.producer;

import lombok.Data;
import lombok.Value;

@Data
@Value
public class ItemOrder {
    String item;
    Double amount;
}
