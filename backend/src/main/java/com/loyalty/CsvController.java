package com.loyalty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import jakarta.validation.Valid;
import com.loyalty.Dtos.*;
import com.loyalty.Models.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
@RestController @RequestMapping("/api/customers") class CsvController {
 final CsvService csv;CsvController(CsvService c){csv=c;}
 @PostMapping(value="/import/preview",consumes="multipart/form-data") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") CsvService.Preview preview(@RequestParam MultipartFile file,@RequestParam(defaultValue="NEW_ONLY") String mode,@AuthenticationPrincipal User user)throws IOException{try{return csv.preview(new String(file.getBytes(),StandardCharsets.UTF_8),mode,user);}catch(IOException e){throw new Problem(400,"Невозможно прочитать CSV");}}
 @PostMapping("/import/apply") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") Map<String,Object> apply(@Valid @RequestBody Apply p,@AuthenticationPrincipal User user)throws IOException{return csv.apply(p.previewId(),user);}
 @GetMapping(value="/export",produces="text/csv") ResponseEntity<String> export(@ModelAttribute CustomerFilter filter,@RequestParam(defaultValue="createdAt,desc") String sort)throws IOException{return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=customers.csv").contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(csv.export(filter,sort));}
}
