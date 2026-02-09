/**
 * Google Apps Script for Contract-First Integration Presentation
 *
 * This script creates a professional Google Slides presentation
 * about Contract-First Integration patterns.
 *
 * How to use:
 * 1. Open Google Slides (slides.google.com)
 * 2. Tools > Script editor
 * 3. Paste this code
 * 4. Run createPresentation()
 * 5. Authorize the script
 * 6. Check your Google Drive for the new presentation
 *
 * Author: Wallace Espindola
 * Email: wallace.espindola@gmail.com
 * LinkedIn: linkedin.com/in/wallaceespindola
 */

// Modern color palette
const COLORS = {
  primary: '#0066CC',       // Blue
  secondary: '#FF5722',     // Orange accent
  dark: '#212121',          // Almost black
  light: '#FAFAFA',         // Off-white
  codeBg: '#282C34',        // Dark code background
  success: '#4CAF50',       // Green
  white: '#FFFFFF'
};

/**
 * Main function to create the presentation
 */
function createPresentation() {
  // Create new presentation
  const presentation = SlidesApp.create('Contract-First Integration - Wallace Espindola');
  const presentationId = presentation.getId();

  Logger.log('Creating presentation: ' + presentationId);

  // Set page size to 16:9
  const pageWidth = 720;  // 10 inches * 72
  const pageHeight = 405; // 5.625 inches * 72

  // Get all slides (there's one blank slide by default)
  const slides = presentation.getSlides();

  // Delete default slide
  slides[0].remove();

  // Add slides
  addTitleSlide(presentation);
  addWhatIsContractFirstSlide(presentation);
  addProblemSlide(presentation);
  addSolutionSlide(presentation);
  addThreeContractTypesSlide(presentation);
  addRepositoryLayoutSlide(presentation);
  addRESTContractSlide(presentation);
  addRESTImplementationSlide(presentation);
  addConsumerParallelWorkSlide(presentation);
  addKafkaTopicSlide(presentation);
  addKafkaAvroSlide(presentation);
  addKafkaProducerSlide(presentation);
  addKafkaConsumerSlide(presentation);
  addDatabaseMigrationSlide(presentation);
  addSchemaEvolutionSlide(presentation);
  addEndToEndFlowSlide(presentation);
  addServiceImplementationSlide(presentation);
  addParallelDevelopmentSlide(presentation);
  addCICDGatesSlide(presentation);
  addPracticalRulesSlide(presentation);
  addMentalModelSlide(presentation);
  addCoordinationToGovernanceSlide(presentation);
  addBenefitsSlide(presentation);
  addRealWorldImpactSlide(presentation);
  addGettingStartedSlide(presentation);
  addToolsSlide(presentation);
  addBestPracticesSlide(presentation);
  addCommonPitfallsSlide(presentation);
  addAdvancedPatternsSlide(presentation);
  addCaseStudySlide(presentation);
  addCICDIntegrationSlide(presentation);
  addMonitoringSlide(presentation);
  addMigrationStrategySlide(presentation);
  addTeamOrganizationSlide(presentation);
  addSuccessMetricsSlide(presentation);
  addResourcesSlide(presentation);
  addKeyTakeawaysSlide(presentation);
  addCallToActionSlide(presentation);
  addQuestionsSlide(presentation);

  Logger.log('Presentation created successfully!');
  Logger.log('Open: https://docs.google.com/presentation/d/' + presentationId);

  return presentation;
}

/**
 * Add title slide
 */
function addTitleSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.BLANK);

  // Background
  const background = slide.getBackground();
  background.setSolidFill(COLORS.primary);

  // Title
  const titleBox = slide.insertTextBox('Contract-First Integration');
  titleBox.setLeft(50).setTop(150).setWidth(620).setHeight(100);
  const titleText = titleBox.getText();
  titleText.getTextStyle()
    .setFontSize(60)
    .setBold(true)
    .setForegroundColor(COLORS.white);
  titleText.getParagraphStyle().setParagraphAlignment(SlidesApp.ParagraphAlignment.CENTER);

  // Subtitle
  const subtitleBox = slide.insertTextBox('Enabling Parallel Systems Development');
  subtitleBox.setLeft(50).setTop(260).setWidth(620).setHeight(60);
  const subtitleText = subtitleBox.getText();
  subtitleText.getTextStyle()
    .setFontSize(32)
    .setForegroundColor(COLORS.white);
  subtitleText.getParagraphStyle().setParagraphAlignment(SlidesApp.ParagraphAlignment.CENTER);

  // Author info
  const authorText = 'Wallace Espindola\nSenior Software Engineer\n\nwallace.espindola@gmail.com\nlinkedin.com/in/wallaceespindola\ngithub.com/wallaceespindola';
  const authorBox = slide.insertTextBox(authorText);
  authorBox.setLeft(50).setTop(350).setWidth(620).setHeight(120);
  const author = authorBox.getText();
  author.getTextStyle()
    .setFontSize(18)
    .setForegroundColor(COLORS.white);
  author.getParagraphStyle().setParagraphAlignment(SlidesApp.ParagraphAlignment.CENTER);
}

/**
 * Add "What is Contract-First?" slide
 */
function addWhatIsContractFirstSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);

  // Title
  const shapes = slide.getShapes();
  shapes[0].getText().setText('What is Contract-First?');
  formatTitle(shapes[0]);

  // Content
  const content = 'Contract-First = Define the integration boundary FIRST, then implement code that conforms to it.\n\n' +
    'A contract includes:\n' +
    '• Operations (endpoints, topics)\n' +
    '• Data shapes (request/response, events)\n' +
    '• Validation rules\n' +
    '• Error models\n' +
    '• Security requirements\n' +
    '• Non-functional rules (timeouts, retries, idempotency)\n' +
    '• Versioning strategy\n\n' +
    'Key Principle: The contract is the SINGLE SOURCE OF TRUTH';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

/**
 * Add "The Problem" slide
 */
function addProblemSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);

  const shapes = slide.getShapes();
  shapes[0].getText().setText('The Problem: Traditional Integration');
  formatTitle(shapes[0]);

  const content = 'System A ---- [waiting...] ----> System B\n' +
    '      ↓\n' +
    'Team A: "When will B be ready?"\n' +
    'Team B: "We\'re still implementing..."\n' +
    '      ↓\n' +
    'Integration happens LATE\n' +
    'Surprises emerge\n' +
    'Assumptions drift\n\n' +
    'Result: Serial dependency, delayed integration, late surprises';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

/**
 * Add "The Solution" slide
 */
function addSolutionSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);

  const shapes = slide.getShapes();
  shapes[0].getText().setText('The Solution: Contract-First');
  formatTitle(shapes[0]);

  const content = '1. Agree on contract (API/event schemas)\n' +
    '   ↓\n' +
    '2. Generate stubs/clients/mocks from contract\n' +
    '   ↓\n' +
    '3. System A implements provider (parallel)\n' +
    '4. System B implements consumer (parallel)\n' +
    '   ↓\n' +
    '5. CI enforces contract alignment\n\n' +
    'Result: Parallel development, early validation, no surprises';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

/**
 * Add "Three Contract Types" slide
 */
function addThreeContractTypesSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);

  const shapes = slide.getShapes();
  shapes[0].getText().setText('Three Contract Types in Java');
  formatTitle(shapes[0]);

  const content = '1️⃣ REST API Contract\n' +
    '• Format: OpenAPI 3.0 YAML/JSON\n' +
    '• Generates: Server stubs, DTOs, client SDKs, mock servers\n\n' +
    '2️⃣ Kafka/Event Contract\n' +
    '• Format: Avro/Protobuf schemas + AsyncAPI\n' +
    '• Generates: Java classes, consumer fixtures\n\n' +
    '3️⃣ Database Contract\n' +
    '• Format: Flyway SQL migrations\n' +
    '• Generates: Versioned, repeatable deployments';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

/**
 * Helper function to format title
 */
function formatTitle(shape) {
  const text = shape.getText();
  text.getTextStyle()
    .setFontSize(36)
    .setBold(true)
    .setForegroundColor(COLORS.dark);
}

/**
 * Helper function to format content
 */
function formatContent(shape) {
  const text = shape.getText();
  text.getTextStyle()
    .setFontSize(22)
    .setForegroundColor(COLORS.dark);
}

/**
 * Add code slide helper
 */
function addCodeSlide(presentation, title, code, language) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.BLANK);

  // Title
  const titleBox = slide.insertTextBox(title);
  titleBox.setLeft(30).setTop(20).setWidth(660).setHeight(50);
  const titleText = titleBox.getText();
  titleText.getTextStyle()
    .setFontSize(32)
    .setBold(true)
    .setForegroundColor(COLORS.dark);

  // Code box
  const codeBox = slide.insertTextBox(code);
  codeBox.setLeft(30).setTop(80).setWidth(660).setHeight(310);

  // Set code background
  codeBox.getFill().setSolidFill(COLORS.codeBg);

  const codeText = codeBox.getText();
  codeText.getTextStyle()
    .setFontSize(16)
    .setFontFamily('Consolas')
    .setForegroundColor('#ABB2BF');  // Light gray for code
}

// Additional slide functions (abbreviated for space)
function addRepositoryLayoutSlide(presentation) {
  addCodeSlide(presentation, 'Repository Layout',
    'repo/\n  contracts/\n    openapi/\n      orders-api.v1.yaml\n    events/\n      avro/\n        OrderCreated.v1.avsc\n      topics.md\n    db/\n      flyway/\n        V1__create_orders.sql\n  service/\n    src/main/java/...',
    'Structure');
}

function addRESTContractSlide(presentation) {
  const code = 'paths:\n  /v1/orders:\n    post:\n      operationId: createOrder\n      responses:\n        \'201\':\n          description: Created\n        \'400\':\n          description: Validation error\n        \'409\':\n          description: Idempotency conflict';
  addCodeSlide(presentation, 'REST Contract Example', code, 'YAML');
}

function addRESTImplementationSlide(presentation) {
  const code = '@RestController\n@RequestMapping("/v1/orders")\npublic class OrdersController {\n\n  @PostMapping\n  public ResponseEntity<OrderResponse> create(\n      @RequestBody CreateOrderRequest request) {\n    OrderResponse created = service.createOrder(request);\n    return ResponseEntity.status(201).body(created);\n  }\n}';
  addCodeSlide(presentation, 'REST Implementation (Java/Spring)', code, 'Java');
}

function addConsumerParallelWorkSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Consumer Parallel Work');
  formatTitle(shapes[0]);

  const content = 'System B doesn\'t wait for System A!\n\n' +
    '✅ Generate Java client from OpenAPI spec immediately\n' +
    '✅ Run mock server for development\n' +
    '✅ Build integration tests against mock\n' +
    '✅ Switch to real API when ready\n\n' +
    'No runtime dependency on the provider being finished';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addKafkaTopicSlide(presentation) {
  const code = '# Topic: orders.order-created.v1\n\n- Purpose: Emitted when order created\n- Key: orderId (ordering per order)\n- Delivery: at-least-once\n- Consumer: idempotent processing\n- Retry: consumer retries errors\n- DLQ: orders.order-created.v1.dlq\n- Compatibility: backward compatible';
  addCodeSlide(presentation, 'Kafka Contract: Topic Semantics', code, 'Markdown');
}

function addKafkaAvroSlide(presentation) {
  const code = '{\n  "type": "record",\n  "name": "OrderCreated",\n  "namespace": "com.acme.events",\n  "fields": [\n    { "name": "eventId", "type": "string" },\n    { "name": "orderId", "type": "string" },\n    { "name": "customerId", "type": "string" },\n    { "name": "source", \n      "type": ["null", "string"],\n      "default": null }\n  ]\n}';
  addCodeSlide(presentation, 'Kafka Contract: Avro Schema', code, 'JSON');
}

function addKafkaProducerSlide(presentation) {
  const code = 'public class OrderEventPublisher {\n\n  private final KafkaTemplate<String, OrderCreated> kafka;\n\n  public void publishOrderCreated(OrderCreated event) {\n    kafka.send(\n      "orders.order-created.v1",\n      event.getOrderId(),\n      event\n    );\n  }\n}';
  addCodeSlide(presentation, 'Kafka Producer (Java)', code, 'Java');
}

function addKafkaConsumerSlide(presentation) {
  const code = '@KafkaListener(topics = "orders.order-created.v1")\npublic void onOrderCreated(OrderCreated event) {\n  // Check if already processed\n  if (processedEventsRepo.existsByEventId(\n      event.getEventId())) {\n    return; // skip duplicate\n  }\n\n  // Process event\n  billingService.createInvoice(...);\n\n  // Store eventId\n  processedEventsRepo.save(event.getEventId());\n}';
  addCodeSlide(presentation, 'Kafka Consumer with Idempotency', code, 'Java');
}

function addDatabaseMigrationSlide(presentation) {
  const code = 'CREATE TABLE orders (\n  id           VARCHAR(32) PRIMARY KEY,\n  customer_id  VARCHAR(32) NOT NULL,\n  status       VARCHAR(16) NOT NULL,\n  created_at   TIMESTAMP NOT NULL\n);\n\nCREATE TABLE order_items (\n  order_id  VARCHAR(32) REFERENCES orders(id),\n  sku       VARCHAR(64) NOT NULL,\n  quantity  INT NOT NULL,\n  PRIMARY KEY (order_id, sku)\n);';
  addCodeSlide(presentation, 'Database Contract: Flyway Migration', code, 'SQL');
}

function addSchemaEvolutionSlide(presentation) {
  const code = '-- V2__add_order_source.sql (Expand)\nALTER TABLE orders ADD COLUMN source VARCHAR(32);\n\n-- Backfill (Migrate)\nUPDATE orders SET source = \'UNKNOWN\'\nWHERE source IS NULL;\n\n-- Enforce (Contract)\nALTER TABLE orders\nALTER COLUMN source SET NOT NULL;';
  addCodeSlide(presentation, 'Schema Evolution: Expand/Migrate/Contract', code, 'SQL');
}

function addEndToEndFlowSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('End-to-End Flow');
  formatTitle(shapes[0]);

  const content = '1. REST API Request\n' +
    '   ↓ (OpenAPI validation)\n' +
    '2. Persist to Database\n' +
    '   ↓ (Flyway schema)\n' +
    '3. Publish Kafka Event\n' +
    '   ↓ (Avro schema + Schema Registry)\n' +
    '4. Consumers Process Event\n' +
    '   (with idempotency)\n\n' +
    'Three contracts working together';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addServiceImplementationSlide(presentation) {
  const code = '@Transactional\npublic OrderResponse createOrder(CreateOrderRequest req) {\n  // 1. Persist (DB contract)\n  ordersRepository.save(...);\n  orderItemsRepository.saveAll(...);\n\n  // 2. Publish event (Kafka contract)\n  OrderCreated event = OrderCreated.newBuilder()\n      .setEventId(UUID.randomUUID().toString())\n      .setOrderId(orderId)\n      .build();\n  publisher.publishOrderCreated(event);\n\n  // 3. Return (REST contract)\n  return new OrderResponse(...);\n}';
  addCodeSlide(presentation, 'Service Implementation', code, 'Java');
}

function addParallelDevelopmentSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Parallel Development Scenarios');
  formatTitle(shapes[0]);

  const content = '🔹 REST Integration\n' +
    'Provider builds server, Consumer builds client using SDK\n' +
    'No blocking - both work independently\n\n' +
    '🔹 Event-Driven Integration\n' +
    'Publisher and consumer build against same Avro schema\n' +
    'No runtime dependency on each other\n\n' +
    '🔹 Legacy ↔ New System\n' +
    'Legacy exposes contract, New system builds against it\n' +
    'Contract serves as the "bridge"';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addCICDGatesSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('CI/CD Gates: Making Contracts Real');
  formatTitle(shapes[0]);

  const content = '🛡️ REST: OpenAPI Breaking Change Detection\n' +
    'Run OpenAPI diff tool in CI\n\n' +
    '🛡️ Kafka: Schema Compatibility Check\n' +
    'Enforce backward compatibility in Schema Registry\n\n' +
    '🛡️ DB: Migration Validation\n' +
    'Flyway validates migrations\n\n' +
    '🛡️ Consumer-Driven Contract Tests\n' +
    'Tools like Pact validate expectations';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addPracticalRulesSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Practical Rules in Contracts');
  formatTitle(shapes[0]);

  const content = 'Versioning\n' +
    '• REST: /v1, /v2 or semantic versioning\n' +
    '• Kafka: compatibility policy + versioned topics\n\n' +
    'Errors & Retries\n' +
    '• REST: standardized ErrorResponse\n' +
    '• Kafka: retry policy + DLQ envelope\n\n' +
    'Idempotency\n' +
    '• REST: Idempotency-Key header\n' +
    '• Kafka: eventId + consumer dedupe';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addMentalModelSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Mental Model Shift');
  formatTitle(shapes[0]);

  const content = 'Without Contract-First\n' +
    '❌ Serial dependency\n' +
    '❌ Teams block each other\n' +
    '❌ Late integration surprises\n\n' +
    'With Contract-First\n' +
    '✅ Parallel development\n' +
    '✅ Early validation\n' +
    '✅ No surprises\n' +
    '✅ CI enforces alignment';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addCoordinationToGovernanceSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('From Coordination to Governance');
  formatTitle(shapes[0]);

  const content = 'Traditional: Coordination Problem\n' +
    '• Team A waits for Team B\n' +
    '• Manual coordination\n' +
    '• High coupling\n\n' +
    'Contract-First: Governance Problem\n' +
    '• Teams agree on contract upfront\n' +
    '• Generate tools automatically\n' +
    '• CI enforces compliance\n' +
    '• Low coupling, high autonomy';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addBenefitsSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Benefits Summary');
  formatTitle(shapes[0]);

  const content = '✅ Enables Parallel Development\n' +
    'Teams no longer block each other\n\n' +
    '✅ Reduces Integration Risk\n' +
    'Contracts catch breaking changes early\n\n' +
    '✅ Improves Evolution\n' +
    'Versioning is explicit\n\n' +
    '✅ Essential for Microservices\n' +
    'Clear boundaries enable scaling';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addRealWorldImpactSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Real-World Impact');
  formatTitle(shapes[0]);

  const content = 'Before Contract-First:\n' +
    '🕐 Integration: 2-3 weeks waiting + 1 week debugging\n' +
    '🐛 Breaking changes in production\n' +
    '👥 Constant coordination\n\n' +
    'After Contract-First:\n' +
    '🕐 Integration: 1 day + parallel work\n' +
    '🐛 Breaking changes caught in CI\n' +
    '👥 Independent work\n\n' +
    'Result: 10x faster, zero surprises';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addGettingStartedSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Getting Started');
  formatTitle(shapes[0]);

  const content = '1. Choose a contract type (REST, Kafka, or DB)\n\n' +
    '2. Define the contract first (before any code)\n\n' +
    '3. Generate tools (stubs, clients, classes)\n\n' +
    '4. Build independently\n\n' +
    '5. Add CI gates\n\n' +
    '6. Evolve safely (versioning + compatibility)';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addToolsSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Tools & Technologies');
  formatTitle(shapes[0]);

  const content = 'REST Contracts\n' +
    '• OpenAPI 3.0, openapi-generator, Swagger UI, Prism\n\n' +
    'Kafka Contracts\n' +
    '• Apache Avro, Confluent Schema Registry, AsyncAPI\n\n' +
    'Database Contracts\n' +
    '• Flyway, Liquibase, Version control';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addBestPracticesSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Best Practices');
  formatTitle(shapes[0]);

  const content = '✅ DO:\n' +
    '• Version all contracts explicitly\n' +
    '• Generate code from contracts\n' +
    '• Enforce compatibility in CI\n' +
    '• Test contract compliance\n\n' +
    '❌ DON\'T:\n' +
    '• Hand-write DTOs (generate them!)\n' +
    '• Skip schema validation\n' +
    '• Make breaking changes without version bump';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addCommonPitfallsSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Common Pitfalls');
  formatTitle(shapes[0]);

  const content = '❌ "We\'ll add the contract later"\n' +
    'Wrong: Contract MUST come first\n\n' +
    '❌ "Just make a quick change"\n' +
    'Wrong: All changes go through contract + CI\n\n' +
    '❌ "Our team doesn\'t need contracts"\n' +
    'Wrong: Even single-team projects benefit\n\n' +
    '❌ "Contracts slow us down"\n' +
    'Wrong: Contracts enable speed through parallelism';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addAdvancedPatternsSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Advanced Patterns');
  formatTitle(shapes[0]);

  const content = 'Idempotency Strategies\n' +
    '• REST: Idempotency-Key header + request hash\n' +
    '• Kafka: eventId + processed events table\n\n' +
    'Schema Evolution\n' +
    '• Add fields with defaults (backward compatible)\n' +
    '• Expand → Migrate → Contract pattern\n\n' +
    'Dead Letter Queues\n' +
    '• Envelope schema for DLQ messages\n' +
    '• Error classification (retriable vs non-retriable)';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addCaseStudySlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Case Study: Order System');
  formatTitle(shapes[0]);

  const content = 'Contracts:\n' +
    '• REST: orders-api.v1.yaml (create, get orders)\n' +
    '• Kafka: OrderCreated.v1.avsc (event)\n' +
    '• DB: V1__create_orders.sql (schema)\n\n' +
    'Teams:\n' +
    '• Orders Team: REST API + publisher\n' +
    '• Billing Team: Kafka consumer\n' +
    '• Fulfillment Team: Kafka consumer\n\n' +
    'Result: All teams work in parallel from day 1';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addCICDIntegrationSlide(presentation) {
  const code = '# GitHub Actions\nname: Contract Validation\n\njobs:\n  openapi-check:\n    - run: npx openapi-diff \\\n        main/orders-api.v1.yaml \\\n        ${{ github.ref }}/orders-api.v1.yaml\n\n  schema-compatibility:\n    - run: mvn schema-registry:test-compatibility';
  addCodeSlide(presentation, 'Integration with CI/CD', code, 'YAML');
}

function addMonitoringSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Monitoring & Observability');
  formatTitle(shapes[0]);

  const content = 'Track Contract Compliance:\n' +
    '• Schema Registry compatibility violations\n' +
    '• OpenAPI spec violations\n' +
    '• Failed Flyway migrations\n\n' +
    'Metrics to Monitor:\n' +
    '• Contract update frequency\n' +
    '• Breaking change attempts blocked\n' +
    '• Consumer lag (Kafka)\n' +
    '• API error rates';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addMigrationStrategySlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Migration Strategy');
  formatTitle(shapes[0]);

  const content = 'Phase 1: Adopt for New Features\n' +
    'Start with new APIs/events only\n\n' +
    'Phase 2: Retrofit Critical Paths\n' +
    'Add contracts to high-traffic integrations\n\n' +
    'Phase 3: Mandate for All\n' +
    'Make contract-first mandatory\n\n' +
    'Phase 4: Automate Governance\n' +
    'CI/CD enforces compliance\n\n' +
    'Tip: Start small, prove value, expand';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addTeamOrganizationSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Team Organization');
  formatTitle(shapes[0]);

  const content = 'Architecture Team\n' +
    '↓ Defines contract standards\n\n' +
    'Provider Team | Consumer Team 1 | Consumer Team 2\n' +
    '↓\n' +
    'CI enforces contracts\n\n' +
    'Clear separation of concerns\n' +
    'Automated enforcement\n' +
    'Independent deployment';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addSuccessMetricsSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Success Metrics');
  formatTitle(shapes[0]);

  const content = 'Development Speed\n' +
    '• Time to integrate: -80% (weeks → days)\n' +
    '• Parallel work enabled: +300%\n\n' +
    'Quality\n' +
    '• Production bugs: -95%\n' +
    '• Breaking changes caught: 100%\n\n' +
    'Team Autonomy\n' +
    '• Coordination meetings: -70%\n' +
    '• Independent deployment: 100%';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addResourcesSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Resources');
  formatTitle(shapes[0]);

  const content = 'Documentation\n' +
    '• OpenAPI Specification: openapis.org\n' +
    '• AsyncAPI: asyncapi.com\n' +
    '• Apache Avro: avro.apache.org\n\n' +
    'Tools\n' +
    '• openapi-generator.tech\n' +
    '• Postman / Insomnia\n' +
    '• Schema Registry\n\n' +
    'Books\n' +
    '• "Building Microservices" - Sam Newman\n' +
    '• "Release It!" - Michael Nygard';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addKeyTakeawaysSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.BLANK);

  // Background
  slide.getBackground().setSolidFill(COLORS.primary);

  // Title
  const titleBox = slide.insertTextBox('Key Takeaways');
  titleBox.setLeft(50).setTop(30).setWidth(620).setHeight(60);
  const titleText = titleBox.getText();
  titleText.getTextStyle()
    .setFontSize(48)
    .setBold(true)
    .setForegroundColor(COLORS.white);
  titleText.getParagraphStyle().setParagraphAlignment(SlidesApp.ParagraphAlignment.CENTER);

  // Content
  const content = '1️⃣ Contract-First = Parallel Development\n\n' +
    '2️⃣ Three Contract Types: REST, Kafka, DB\n\n' +
    '3️⃣ CI Enforces Alignment\n\n' +
    '4️⃣ Evolution is Built-In\n\n' +
    '5️⃣ From Coordination to Governance';

  const contentBox = slide.insertTextBox(content);
  contentBox.setLeft(50).setTop(120).setWidth(620).setHeight(250);
  const contentText = contentBox.getText();
  contentText.getTextStyle()
    .setFontSize(26)
    .setForegroundColor(COLORS.white);
}

function addCallToActionSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.TITLE_AND_BODY);
  const shapes = slide.getShapes();
  shapes[0].getText().setText('Call to Action');
  formatTitle(shapes[0]);

  const content = '🚀 Start Today:\n\n' +
    '1. Pick one integration to convert\n' +
    '2. Write the contract FIRST\n' +
    '3. Generate tools from contract\n' +
    '4. Add CI validation\n' +
    '5. Measure the impact\n\n' +
    '📚 Learn More:\n' +
    'github.com/wallaceespindola/contract-first-integrations';

  shapes[1].getText().setText(content);
  formatContent(shapes[1]);
}

function addQuestionsSlide(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.BLANK);

  // Background
  slide.getBackground().setSolidFill(COLORS.primary);

  // Title
  const titleBox = slide.insertTextBox('Questions?');
  titleBox.setLeft(50).setTop(100).setWidth(620).setHeight(100);
  const titleText = titleBox.getText();
  titleText.getTextStyle()
    .setFontSize(60)
    .setBold(true)
    .setForegroundColor(COLORS.white);
  titleText.getParagraphStyle().setParagraphAlignment(SlidesApp.ParagraphAlignment.CENTER);

  // Contact info
  const contactText = 'Wallace Espindola\nSenior Software Engineer\n\n' +
    '📧 wallace.espindola@gmail.com\n' +
    '🔗 linkedin.com/in/wallaceespindola\n' +
    '💻 github.com/wallaceespindola\n\n' +
    'This Presentation:\n' +
    'github.com/wallaceespindola/contract-first-integrations\n\n' +
    'Thank you! 🙏';

  const contactBox = slide.insertTextBox(contactText);
  contactBox.setLeft(50).setTop(220).setWidth(620).setHeight(200);
  const contact = contactBox.getText();
  contact.getTextStyle()
    .setFontSize(20)
    .setForegroundColor(COLORS.white);
  contact.getParagraphStyle().setParagraphAlignment(SlidesApp.ParagraphAlignment.CENTER);
}
