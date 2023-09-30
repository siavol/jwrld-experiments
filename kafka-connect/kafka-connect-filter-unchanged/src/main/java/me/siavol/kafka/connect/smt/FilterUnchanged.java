package me.siavol.kafka.connect.smt;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;
import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public abstract class FilterUnchanged<R extends ConnectRecord<R>>  implements Transformation<R> {
    private static final Logger logger = LoggerFactory.getLogger(FilterUnchanged.class);
    public static final String OVERVIEW_DOC = "Filters unchanged messages";
    private static final String ALL_FIELDS = "*";

    private interface ConfigName {
        String BEFORE_FIELD_NAME = "before.field.name";
        String AFTER_FIELD_NAME = "after.field.name";
        String COMPARE_FIELDS = "compare.fields";
        String IGNORE_FIELDS = "ignore.fields";
    }

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(ConfigName.BEFORE_FIELD_NAME, ConfigDef.Type.STRING, "before", ConfigDef.Importance.MEDIUM, "Field name for before state.")
            .define(ConfigName.AFTER_FIELD_NAME, ConfigDef.Type.STRING, "after", ConfigDef.Importance.MEDIUM, "Field name for after state.")
            .define(ConfigName.COMPARE_FIELDS, ConfigDef.Type.STRING, ALL_FIELDS, ConfigDef.Importance.MEDIUM, "Fields to be compared to identify unchanged.")
            .define(ConfigName.IGNORE_FIELDS, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM, "Fields to be ignored to identify unchanged.");

    private static final String PURPOSE = "adding UUID to record";

    private String beforeFieldName;
    private String afterFieldName;
    private String compareFields;
    private String ignoreFields;

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void configure(Map<String, ?> props) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, props);
        beforeFieldName = config.getString(ConfigName.BEFORE_FIELD_NAME);
        afterFieldName = config.getString(ConfigName.AFTER_FIELD_NAME);
        compareFields = config.getString(ConfigName.COMPARE_FIELDS);
        ignoreFields = config.getString(ConfigName.IGNORE_FIELDS);
    }

    @Override
    public R apply(R record) {
        logger.debug("Applying record");
        try {
            if (operatingSchema(record) == null) {
                return applySchemaless(record);
            } else {
                return applyWithSchema(record);
            }
        } catch (Exception e) {
            logger.error("Failed to apply filter unchanged", e);
            throw e;
        }
    }

    @Override
    public void close() {
    }

    private R applySchemaless(R record) {
        logger.info("Applying schemaless");
        final Map<String, Object> value = requireMap(operatingValue(record), PURPOSE);
        final Map<String, Object> updatedValue = new HashMap<>(value);

        updatedValue.put("kafka-connect-filter-unchanged", "Schemaless records filtering is not supported.");
        return newRecord(record, updatedValue);
    }

    private R applyWithSchema(R record) {
        logger.debug("Applying with schema");
        final Struct value = requireStruct(operatingValue(record), PURPOSE);

        Struct before = value.getStruct(beforeFieldName);
        Struct after = value.getStruct(afterFieldName);

        String[] fieldsToCompare = getFieldsToCompare(before);
        logger.debug("Fields to compare " + Arrays.toString(fieldsToCompare));
        Set<String> fieldsToIgnore = Arrays.stream(ignoreFields.split(",")).collect(Collectors.toSet());
        logger.debug("Fields to ignore " + fieldsToIgnore);

        for (String fieldName : fieldsToCompare) {
            if (fieldsToIgnore.contains(fieldName)) {
                logger.debug("Ignore field " + fieldName);
                continue;
            }
            boolean valuesAreDifferent = isValuesAreDifferent(fieldName, before, after);
            if (valuesAreDifferent) {
                logger.debug("Changes in the field " + fieldName);
                return record;
            }
        }

        logger.debug("Data is unchanged");
        return null;
    }

    private static boolean isValuesAreDifferent(String fieldName, Struct before, Struct after) {
        try {
            logger.info("Compare field " + fieldName);

            Object beforeValue = before.get(fieldName);
            Object afterValue = after.get(fieldName);

            return !beforeValue.equals(afterValue);
        } catch (Exception e) {
            logger.error("Failed to compare field values " + fieldName, e);
            throw e;
        }
    }

    private String[] getFieldsToCompare(Struct before) {
        Schema beforeSchema = before.schema();
        List<Field> schemaFields = beforeSchema.fields();

        return compareFields.equals(ALL_FIELDS)
                ? schemaFields.stream().map(Field::name).toArray(String[]::new)
                : compareFields.split(",");
    }

    protected abstract Schema operatingSchema(R record);
    protected abstract Object operatingValue(R record);
    protected abstract R newRecord(R record, Object updatedValue);

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
        protected R newRecord(R record, Object updatedValue) {
            return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), null, updatedValue, record.timestamp());
        }
    }
}
