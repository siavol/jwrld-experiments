package me.siavol.kafka.connect.smt;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;
import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public abstract class FilterUnchanged<R extends ConnectRecord<R>>  implements Transformation<R> {
    public static final String OVERVIEW_DOC = "Filters unchanged messages";

    private interface ConfigName {
        String BEFORE_FIELD_NAME = "before.field.name";
        String AFTER_FIELD_NAME = "after.field.name";
    }

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(ConfigName.BEFORE_FIELD_NAME, ConfigDef.Type.STRING, "before", ConfigDef.Importance.MEDIUM, "Field name for before state")
            .define(ConfigName.AFTER_FIELD_NAME, ConfigDef.Type.STRING, "after", ConfigDef.Importance.MEDIUM, "Field name for after state");

    private static final String PURPOSE = "adding UUID to record";

    private String beforeFieldName;
    private String afterFieldName;

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void configure(Map<String, ?> props) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, props);
        beforeFieldName = config.getString(ConfigName.BEFORE_FIELD_NAME);
        afterFieldName = config.getString(ConfigName.AFTER_FIELD_NAME);
    }

    @Override
    public R apply(R record) {
        if (operatingSchema(record) == null) {
            return applySchemaless(record);
        } else {
            return applyWithSchema(record);
        }
    }

    @Override
    public void close() {
    }

    private R applySchemaless(R record) {
        final Map<String, Object> value = requireMap(operatingValue(record), PURPOSE);
        final Map<String, Object> updatedValue = new HashMap<>(value);

        updatedValue.put("kafka-connect-filter-unchanged", "Schemaless records filtering is not supported.");
        return newRecord(record, null, updatedValue);
    }

    private R applyWithSchema(R record) {
        final Struct value = requireStruct(operatingValue(record), PURPOSE);

        Struct before = value.getStruct(beforeFieldName);
        Struct after = value.getStruct(afterFieldName);

        Object beforeRating = before.get("rating");
        Object afterRating = after.get("rating");

        if (beforeRating.equals(afterRating)) {
            return null;
        } else {
            return record;
        }

    }

    protected abstract Schema operatingSchema(R record);
    protected abstract Object operatingValue(R record);
    protected abstract R newRecord(R record, Schema updatedSchema, Object updatedValue);

    public static class Value<R extends ConnectRecord<R>> extends FilterUnchanged<R> {

        @Override
        protected Schema operatingSchema(R record) {
            return record.valueSchema();
        }

        @Override
        protected Object operatingValue(R record) {
            return record.value();
        }

        @Override
        protected R newRecord(R record, Schema updatedSchema, Object updatedValue) {
            return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), updatedSchema, updatedValue, record.timestamp());
        }
    }
}
