package net.ent.etnc.orbit.dtos.requestDTO;

import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResoudreDefaillanceRequestDto {
    @Length(max = 500, message = "noteSolution doit avoir au plus 500 caractères")
    private String noteSolution;
}
