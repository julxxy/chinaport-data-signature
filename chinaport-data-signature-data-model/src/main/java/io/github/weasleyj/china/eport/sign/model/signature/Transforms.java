package io.github.weasleyj.china.eport.sign.model.signature;

import io.github.weasleyj.china.eport.sign.constants.NameSpace;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Transforms", namespace = NameSpace.NAMESPACE_DS_URI)
public class Transforms implements Serializable {

    @XmlElement(name = "Transform", namespace = NameSpace.NAMESPACE_DS_URI)
    private Transform transform = new Transform();
}
