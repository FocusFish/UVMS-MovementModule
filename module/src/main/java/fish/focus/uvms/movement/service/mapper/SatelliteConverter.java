package fish.focus.uvms.movement.service.mapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import fish.focus.uvms.movement.model.constants.SatId;

@Converter
public class SatelliteConverter implements AttributeConverter<SatId, Integer> {


        @Override
        public Integer convertToDatabaseColumn(SatId attribute) {
            return attribute.getValue();
        }

        @Override
        public SatId convertToEntityAttribute(Integer dbData) {
            return SatId.fromInt(dbData);

        }
}
