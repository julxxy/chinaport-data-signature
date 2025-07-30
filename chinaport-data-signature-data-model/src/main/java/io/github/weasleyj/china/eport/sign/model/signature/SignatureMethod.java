package io.github.weasleyj.china.eport.sign.model.signature;

import io.github.weasleyj.china.eport.sign.constants.NameSpace;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CanonicalizationMethod", namespace = NameSpace.NAMESPACE_DS_URI)
public class SignatureMethod implements Serializable {

    @XmlAttribute(name = "Algorithm")
    private String Algorithm = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
}
