import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

// @Field makes these accessible from script methods below.
// Plain `def` at script level is local to the run() method and invisible to methods.

@Field final BUNDLE_DEFS = [
    Identifiers: [
        type: 'object',
        properties: [
            externalId: [type: 'string'],
            originalId: [type: 'string'],
            pid:        [type: 'string'],
        ],
        additionalProperties: false,
    ],
    LoggingInfo: [
        type: 'object',
        properties: [
            actionType:   [type: 'string'],
            comment:      [type: 'string'],
            date:         [type: 'string'],
            type:         [type: 'string'],
            userEmail:    [type: 'string'],
            userFullName: [type: 'string'],
            userRole:     [type: 'string'],
        ],
        additionalProperties: false,
    ],
    Metadata: [
        type: 'object',
        properties: [
            modifiedAt:   [type: 'string'],
            modifiedBy:   [type: 'string'],
            published:    [type: 'boolean'],
            registeredAt: [type: 'string'],
            registeredBy: [type: 'string'],
            terms:        [type: 'array', items: [type: 'string']],
        ],
        additionalProperties: false,
    ],
]

@Field final BUNDLE_PROPERTIES = [
    active:               [type: 'boolean'],
    auditState:           [type: 'string'],
    catalogueId:          [type: 'string'],
    draft:                [type: 'boolean'],
    identifiers:          ['$ref': '#/$defs/Identifiers'],
    latestAuditInfo:      ['$ref': '#/$defs/LoggingInfo'],
    latestOnboardingInfo: ['$ref': '#/$defs/LoggingInfo'],
    latestUpdateInfo:     ['$ref': '#/$defs/LoggingInfo'],
    legacy:               [type: 'boolean'],
    loggingInfo:          [type: 'array', items: ['$ref': '#/$defs/LoggingInfo']],
    metadata:             ['$ref': '#/$defs/Metadata'],
    status:               [type: 'string'],
    suspended:            [type: 'boolean'],
]

// ---------------------------------------------------------------------------
// Schema conversion
// ---------------------------------------------------------------------------

def fieldToSchema(field) {
    def ti           = field.typeInfo ?: [:]
    def fieldType    = ti.type
    def multiplicity = ti.multiplicity ?: false
    def values       = ti.values ?: []

    def itemSchema
    if (fieldType == 'composite') {
        itemSchema = fieldsToObjectSchema(field.subFields ?: [])
    } else if (fieldType in ['string', 'richText', 'url', 'email', 'vocabulary', 'date']) {
        itemSchema = [type: 'string']
    } else if (fieldType == 'radio') {
        def ids = values.collect { it.id }.findAll { it }
        if (ids && ids.every { it in ['true', 'false'] }) {
            itemSchema = [type: 'boolean']
        } else if (ids) {
            itemSchema = [type: 'string', enum: ids]
        } else {
            itemSchema = [type: 'string']
        }
    } else if (fieldType == 'select') {
        def ids = values.collect { it.id }.findAll { it }
        itemSchema = ids ? [type: 'string', enum: ids] : [type: 'string']
    } else {
        itemSchema = [:]
    }

    return multiplicity ? [type: 'array', items: itemSchema] : itemSchema
}

def fieldsToObjectSchema(fields) {
    if (!fields) return [type: 'object']   // no sub-fields: open-ended object

    def properties = [:]
    def required   = []

    fields.each { field ->
        def fid = field.id
        if (!fid) return
        properties[fid] = fieldToSchema(field)
        if (field.form?.mandatory) required << fid
    }

    def schema = [type: 'object', properties: properties, additionalProperties: false]
    if (required) schema.required = required
    return schema
}

def collectAllFields(modelData) {
    def fields = []
    (modelData.sections ?: []).each { section ->
        (section.subSections ?: []).each { sub ->
            fields.addAll(sub.fields ?: [])
            (sub.subSections ?: []).each { sub2 ->
                fields.addAll(sub2.fields ?: [])
            }
        }
    }
    return fields
}

def generateResourceSchema(modelData) {
    def schema = fieldsToObjectSchema(collectAllFields(modelData))
    return [('$schema'): 'https://json-schema.org/draft/2020-12/schema'] + schema
}

def generateBundleSchema(resourceClass, payloadField, resourceSchema) {
    def inlineDef = resourceSchema.findAll { k, v -> k != '$schema' }
    return [
        ('$schema'): 'https://json-schema.org/draft/2020-12/schema',
        ('$defs')  : BUNDLE_DEFS + [(resourceClass): inlineDef],
        type       : 'object',
        properties : BUNDLE_PROPERTIES + [(payloadField): ['$ref': "#/\$defs/${resourceClass}"]],
        additionalProperties: false,
    ]
}

// ---------------------------------------------------------------------------
// Cleanup
// ---------------------------------------------------------------------------

def victoolsSchemaNames(domainDir) {
    def names = [] as Set
    if (domainDir.isDirectory()) {
        domainDir.listFiles()
            .findAll { it.name.endsWith('.java') && it.name != 'package-info.java' }
            .each    { names << it.name.replace('.java', '-schema.json') }
    }
    return names
}

def deleteStaleSchemas(schemasDir, domainDir, generated) {
    def valid   = generated + victoolsSchemaNames(domainDir)
    def deleted = []
    schemasDir.listFiles()
        .findAll { it.name.endsWith('-schema.json') }
        .sort    { it.name }
        .each    { file ->
            if (!(file.name in valid)) {
                file.delete()
                deleted << file.name
            }
        }
    if (deleted) {
        println "\nDeleted ${deleted.size()} stale schema(s):"
        deleted.each { println "  deleted: $it" }
    }
}

// ---------------------------------------------------------------------------
// JSON output — 2-space indentation to match project convention
// ---------------------------------------------------------------------------

def toJson(obj) {
    def raw = JsonOutput.prettyPrint(JsonOutput.toJson(obj))
    // Groovy's prettyPrint uses 4-space indent; halve it to get 2-space
    raw.replaceAll(/(?m)^ +/) { spaces -> ' ' * (spaces.length().intdiv(2)) }
}

def writeSchema(file, schema) {
    file.text = toJson(schema) + '\n'
    println "  written: ${file.name}"
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

// When running via Maven plugin, 'basedir' is bound to the module directory.
// When running standalone (groovy scripts/generate-schemas.groovy from project root),
// fall back to the current working directory.
def projectRoot
try {
    projectRoot = basedir.parentFile
} catch (MissingPropertyException ignored) {
    projectRoot = new File('').canonicalFile
}

def modelsDir  = new File(projectRoot, 'resource-catalogue-service/src/main/resources/models')
def schemasDir = new File(projectRoot, 'resource-catalogue-model/src/main/resources/schemas/json')
def domainDir  = new File(projectRoot, 'resource-catalogue-model/src/main/java/gr/uoa/di/madgik/resourcecatalogue/domain')

schemasDir.mkdirs()

def slurper   = new JsonSlurper()
def generated = [] as Set

def modelFiles = modelsDir.listFiles()
    .findAll { it.name.startsWith('m-b-') && it.name.endsWith('.json') }
    .sort    { it.name }

for (modelFile in modelFiles) {
    def modelData     = slurper.parse(modelFile)
    def resourceClass = modelData.resourceClass
    def bundleClass   = modelData.bundleClass
    def payloadField  = modelData.bundlePayloadField

    if (!resourceClass || !bundleClass || !payloadField) {
        println "  SKIP (missing schema metadata): ${modelFile.name}"
        continue
    }

    println "Processing ${modelFile.name} -> ${resourceClass} / ${bundleClass}"

    def resourceSchema = generateResourceSchema(modelData)
    def bundleSchema   = generateBundleSchema(resourceClass, payloadField, resourceSchema)

    // Use + concatenation (not GString interpolation) to get plain java.lang.String.
    // GStrings have a different hashCode than String, so GStrings in a HashSet
    // won't match a String lookup — the 'in' check in deleteStaleSchemas would fail.
    def resourceFile = resourceClass + '-schema.json'
    def bundleFile   = bundleClass   + '-schema.json'

    writeSchema(new File(schemasDir, resourceFile), resourceSchema)
    writeSchema(new File(schemasDir, bundleFile),   bundleSchema)

    generated << resourceFile
    generated << bundleFile
}

deleteStaleSchemas(schemasDir, domainDir, generated)
