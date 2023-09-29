package me.siavol.kafka.connect.smt;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FilterUnchangedTest {
    private FilterUnchanged<SourceRecord> xform = new FilterUnchanged.Value<>();

    @AfterEach
    public void tearDown() {
        xform.close();
    }

    @Nested
    public class RecordsWithSchema {
        @Test
        public void copySchemaAndInsertUuidField() {
            final Map<String, Object> props = new HashMap<>();
            props.put("uuid.field.name", "myUuid");
            xform.configure(props);

            final Schema dataStruct = SchemaBuilder.struct()
                    .name("my-data")
                    .version(1)
                    .field("rating", Schema.OPTIONAL_STRING_SCHEMA)
                    .build();

            final Schema simpleStructSchema = SchemaBuilder.struct()
                    .name("name")
                    .version(1)
                    .doc("doc")
                    .field("magic", Schema.OPTIONAL_INT64_SCHEMA)
                    .field("before", dataStruct)
                    .field("after", dataStruct)
                    .build();
            final Struct simpleStruct = new Struct(simpleStructSchema)
                    .put("magic", 42L)
                    .put("before", new Struct(dataStruct)
                            .put("rating", "G"))
                    .put("after", new Struct(dataStruct)
                            .put("rating", "PG"));

            final SourceRecord record = new SourceRecord(null, null, "test", 0, simpleStructSchema, simpleStruct);
            final SourceRecord transformedRecord = xform.apply(record);

            Assertions.assertEquals(simpleStructSchema.name(), transformedRecord.valueSchema().name());
            Assertions.assertEquals(simpleStructSchema.version(), transformedRecord.valueSchema().version());
            Assertions.assertEquals(simpleStructSchema.doc(), transformedRecord.valueSchema().doc());

            Assertions.assertEquals(Schema.OPTIONAL_INT64_SCHEMA, transformedRecord.valueSchema().field("magic").schema());
            Assertions.assertEquals(42L, ((Struct) transformedRecord.value()).getInt64("magic").longValue());
        }
    }

    @Test
    public void schemalessFilteringNotSupported() {
        final Map<String, Object> props = new HashMap<>();
        props.put("uuid.field.name", "myUuid");
        xform.configure(props);

        final SourceRecord record = new SourceRecord(null, null, "test", 0,
                null, Collections.singletonMap("magic", 42L));

        final SourceRecord transformedRecord = xform.apply(record);
        Assertions.assertEquals(42L, ((Map) transformedRecord.value()).get("magic"));
        Assertions.assertEquals("Schemaless records filtering is not supported.", ((Map) transformedRecord.value()).get("kafka-connect-filter-unchanged"));

    }
}