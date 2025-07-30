package io.github.weasleyj.china.eport.sign.model.request;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Message implements Serializable {
    private MessageHead MessageHead;
    private MessageBody MessageBody;
}
