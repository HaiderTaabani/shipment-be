package com.example.shipment.shipment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

  @Mock
  private ShipmentRepository shipmentRepository;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private ShipmentService shipmentService;

  @Test
  void shouldCreateShipment() {
    var request = ShipmentDTO.CreateShipmentRequest.builder()
        .origin("Paris")
        .destination("Lyon")
        .estimatedDelivery("2026-08-20")
        .build();

    when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
      Shipment shipment = invocation.getArgument(0);
      shipment.setId(1L);
      shipment.onCreate();
      return shipment;
    });

    var response = shipmentService.createShipment(request);

    assertEquals("Paris", response.getOrigin());
    assertEquals("Lyon", response.getDestination());
    assertEquals(ShipmentStatus.ORDER_PLACED, response.getStatus());
    assertTrue(response.getTrackingNumber().startsWith("TRK-"));
    verify(shipmentRepository).save(any(Shipment.class));
  }

  @Test
  void shouldReturnAllShipments() {
    when(shipmentRepository.findAll()).thenReturn(List.of(
        shipment(1L, "TRK-00000001", ShipmentStatus.ORDER_PLACED),
        shipment(2L, "TRK-00000002", ShipmentStatus.IN_TRANSIT)
    ));

    var responses = shipmentService.getAllShipments();

    assertEquals(2, responses.size());
    assertEquals("TRK-00000001", responses.getFirst().getTrackingNumber());
  }

  @Test
  void shouldReturnShipmentById() {
    when(shipmentRepository.findById(1L))
        .thenReturn(Optional.of(shipment(1L, "TRK-00000001", ShipmentStatus.PROCESSING)));

    var response = shipmentService.getShipmentById(1L);

    assertEquals(1L, response.getId());
    assertEquals(ShipmentStatus.PROCESSING, response.getStatus());
  }

  @Test
  void shouldThrowExceptionWhenShipmentDoesNotExist() {
    when(shipmentRepository.findById(99L)).thenReturn(Optional.empty());

    var exception = assertThrows(
        RuntimeException.class,
        () -> shipmentService.getShipmentById(99L)
    );

    assertEquals("Shipment not found with id: 99", exception.getMessage());
  }

  @Test
  void shouldUpdateShipmentStatusAndLocation() {
    var existingShipment = shipment(1L, "TRK-00000001", ShipmentStatus.PROCESSING);
    var request = ShipmentDTO.UpdateStatusRequest.builder()
        .status(ShipmentStatus.IN_TRANSIT)
        .currentLocation("Orléans")
        .build();

    when(shipmentRepository.findById(1L)).thenReturn(Optional.of(existingShipment));
    when(shipmentRepository.save(existingShipment)).thenReturn(existingShipment);

    var response = shipmentService.updateShipmentStatus(request, 1L);

    assertEquals(ShipmentStatus.IN_TRANSIT, response.getStatus());
    assertEquals("Orléans", response.getCurrentLocation());
    verify(shipmentRepository).save(existingShipment);
    verify(messagingTemplate).convertAndSend(eq("/topic/shipments"), any(Object.class));
  }

  private Shipment shipment(Long id, String trackingNumber, ShipmentStatus status) {
    var now = LocalDateTime.now();
    return Shipment.builder()
        .id(id)
        .trackingNumber(trackingNumber)
        .origin("Paris")
        .destination("Lyon")
        .status(status)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
