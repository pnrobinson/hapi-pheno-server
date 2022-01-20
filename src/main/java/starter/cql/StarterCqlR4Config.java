package starter.cql;

import ca.uhn.fhir.cql.config.CqlR4Config;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import starter.annotations.OnR4Condition;

@Conditional({OnR4Condition.class, CqlConfigCondition.class})
@Import({CqlR4Config.class})
public class StarterCqlR4Config {
}
