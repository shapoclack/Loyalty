package com.loyalty;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import com.loyalty.Dtos.*;
import com.loyalty.Models.User;
import java.util.*;
@RestController @RequestMapping("/api/customers") class CustomerController {
 final CustomerService service; CustomerController(CustomerService s){service=s;}
 @GetMapping PageView<CustomerView> list(@ModelAttribute CustomerFilter filter,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt,desc") String sort){return service.list(filter,CustomerFilter.page(page,size,sort));}
 @GetMapping("/{id}") CustomerView one(@PathVariable UUID id){return service.one(id);}
 @PostMapping @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") CustomerView create(@Valid @RequestBody CustomerInput p){return service.create(p);}
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") CustomerView update(@PathVariable UUID id,@Valid @RequestBody CustomerInput p){return service.update(id,p);}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") void archive(@PathVariable UUID id,@RequestParam long version){service.archive(id,version,true);}
 @PostMapping("/{id}/restore") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") void restore(@PathVariable UUID id,@RequestParam long version){service.archive(id,version,false);}
 @DeleteMapping("/{id}/permanent") @PreAuthorize("hasRole('ADMIN')") void permanent(@PathVariable UUID id,@RequestParam long version){service.permanent(id,version);}
 record BulkResult(UUID id,boolean success,String message) {}
 @PostMapping("/bulk") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") List<BulkResult> bulk(@Valid @RequestBody Bulk p){return p.ids().stream().map(id->{try{service.bulkOne(id,p);return new BulkResult(id,true,null);}catch(Problem e){return new BulkResult(id,false,e.getMessage());}catch(org.springframework.dao.DataAccessException e){return new BulkResult(id,false,"Конфликт записи");}}).toList();}
 @GetMapping("/{id}/notes") List<NoteView> notes(@PathVariable UUID id){return service.notes(id);}
 @PostMapping("/{id}/notes") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") NoteView note(@PathVariable UUID id,@Valid @RequestBody NoteInput p,@AuthenticationPrincipal User user){return service.note(id,null,p,user);}
 @PutMapping("/{id}/notes/{noteId}") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") NoteView note(@PathVariable UUID id,@PathVariable UUID noteId,@Valid @RequestBody NoteInput p,@AuthenticationPrincipal User user){return service.note(id,noteId,p,user);}
 @DeleteMapping("/{id}/notes/{noteId}") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") void deleteNote(@PathVariable UUID id,@PathVariable UUID noteId,@RequestParam long version){service.deleteNote(id,noteId,version);}
}
