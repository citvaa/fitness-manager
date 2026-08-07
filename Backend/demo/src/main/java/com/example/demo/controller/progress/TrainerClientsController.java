package com.example.demo.controller.progress;
import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.service.security.AuthenticatedUserService;
import com.example.demo.service.user.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/trainer/clients")
public class TrainerClientsController {
 private final ClientService clientService; private final AuthenticatedUserService authenticatedUser;
 @RoleRequired("TRAINER") @GetMapping public List<ClientSummaryDTO> clients(){return clientService.findTrainedBy(authenticatedUser.trainer().getId());}
}
