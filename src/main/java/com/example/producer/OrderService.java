package com.example.producer;


import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderService {
    private final Producer producer;

    @Autowired
    public OrderService(Producer producer) {
        this.producer = producer;
    }

    public String createFoodOrder(ItemOrder foodOrder) throws JsonProcessingException {
        return producer.sendMessage(foodOrder);
    }
}
