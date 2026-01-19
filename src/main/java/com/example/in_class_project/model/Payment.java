package com.example.in_class_project.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private String id;

    private String orderId;
    private Double amount;
    private String status;
    private String paymentId;
    private String razorpayOrderId;
    private Instant createdAt;
}
