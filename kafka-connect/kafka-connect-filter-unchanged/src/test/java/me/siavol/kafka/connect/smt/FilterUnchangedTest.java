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
    private final FilterUnchanged<SourceRecord> filterUnchanged = new FilterUnchanged.Value<>();

    @AfterEach
    public void tearDown() {
        filterUnchanged.close();
    }

    @Nested
    public class RecordsWithSchema {
        @Test
        public void shouldReturnOriginalRecordWhenDataIsDifferent() {
            filterUnchanged.configure(Map.ofEntries(
                    Map.entry("before.field.name", "v1"),
                    Map.entry("after.field.name", "v2"),
                    Map.entry("compare.fields", "rating")
            ));

            final Schema dataStruct = SchemaBuilder.struct()
                    .name("my-data")
                    .version(1)
                    .field("rating", Schema.OPTIONAL_STRING_SCHEMA)
                    .build();

            final Schema simpleStructSchema = getSimpleStructSchema(dataStruct, "v1", "v2");
            final Struct simpleStruct = new Struct(simpleStructSchema)
                    .put("magic", 42L)
                    .put("v1", new Struct(dataStruct)
                            .put("rating", "G"))
                    .put("v2", new Struct(dataStruct)
                            .put("rating", "PG"));

            final SourceRecord record = new SourceRecord(null, null, "test", 0, simpleStructSchema, simpleStruct);
            final SourceRecord transformedRecord = filterUnchanged.apply(record);

            Assertions.assertSame(record, transformedRecord);
        }

        @Test
        public void shouldReturnNullWhenDataIsUnchanged() {
            filterUnchanged.configure(Map.ofEntries(
                    Map.entry("compare.fields", "rating")
            ));

            final Schema dataStruct = SchemaBuilder.struct()
                    .name("my-data")
                    .version(1)
                    .field("rating", Schema.OPTIONAL_STRING_SCHEMA)
                    .build();

            final Schema simpleStructSchema = getSimpleStructSchema(dataStruct, "before", "after");
            final Struct simpleStruct = new Struct(simpleStructSchema)
                    .put("magic", 42L)
                    .put("before", new Struct(dataStruct)
                            .put("rating", "PG"))
                    .put("after", new Struct(dataStruct)
                            .put("rating", "PG"));

            final SourceRecord record = new SourceRecord(null, null, "test", 0, simpleStructSchema, simpleStruct);
            final SourceRecord transformedRecord = filterUnchanged.apply(record);

            Assertions.assertNull(transformedRecord);
        }

        @Test
        public void shouldReturnOriginalRecordWhenDataIsDifferentInOneFieldOfTwo() {
            filterUnchanged.configure(Map.ofEntries(
                    Map.entry("compare.fields", "rating,name")
            ));

            final Schema dataStruct = SchemaBuilder.struct()
                    .name("my-data")
                    .version(1)
                    .field("rating", Schema.OPTIONAL_INT64_SCHEMA)
                    .field("name", Schema.OPTIONAL_STRING_SCHEMA)
                    .build();

            final Schema simpleStructSchema = getSimpleStructSchema(dataStruct, "before", "after");
            final Struct simpleStruct = new Struct(simpleStructSchema)
                    .put("magic", 42L)
                    .put("before", new Struct(dataStruct)
                            .put("rating", 12L)
                            .put("name", "Alex"))
                    .put("after", new Struct(dataStruct)
                            .put("rating", 42L)
                            .put("name", "Alex"));

            final SourceRecord record = new SourceRecord(null, null, "test", 0, simpleStructSchema, simpleStruct);
            final SourceRecord transformedRecord = filterUnchanged.apply(record);

            Assertions.assertSame(record, transformedRecord);
        }

        @Test
        public void shouldReturnNullWhenDataIsSameInTwoFields() {
            filterUnchanged.configure(Map.ofEntries(
                    Map.entry("compare.fields", "rating,name")
            ));

            final Schema dataStruct = SchemaBuilder.struct()
                    .name("my-data")
                    .version(1)
                    .field("rating", Schema.OPTIONAL_INT64_SCHEMA)
                    .field("name", Schema.OPTIONAL_STRING_SCHEMA)
                    .build();

            final Schema simpleStructSchema = getSimpleStructSchema(dataStruct, "before", "after");
            final Struct simpleStruct = new Struct(simpleStructSchema)
                    .put("magic", 42L)
                    .put("before", new Struct(dataStruct)
                            .put("rating", 3L)
                            .put("name", "Alex"))
                    .put("after", new Struct(dataStruct)
                            .put("rating", 3L)
                            .put("name", "Alex"));

            final SourceRecord record = new SourceRecord(null, null, "test", 0, simpleStructSchema, simpleStruct);
            final SourceRecord transformedRecord = filterUnchanged.apply(record);

            Assertions.assertNull(transformedRecord);
        }

        private Schema getSimpleStructSchema(Schema dataStruct, String before, String after) {
            return SchemaBuilder.struct()
                    .name("name")
                    .version(1)
                    .doc("doc")
                    .field("magic", Schema.OPTIONAL_INT64_SCHEMA)
                    .field(before, dataStruct)
                    .field(after, dataStruct)
                    .build();
        }
    }

    @Test
    public void schemalessFilteringNotSupported() {
        final Map<String, Object> props = new HashMap<>();
        filterUnchanged.configure(props);

        final SourceRecord record = new SourceRecord(null, null, "test", 0,
                null, Collections.singletonMap("magic", 42L));

        final SourceRecord transformedRecord = filterUnchanged.apply(record);
        Assertions.assertEquals(42L, ((Map) transformedRecord.value()).get("magic"));
        Assertions.assertEquals("Schemaless records filtering is not supported.", ((Map) transformedRecord.value()).get("kafka-connect-filter-unchanged"));

    }
}
