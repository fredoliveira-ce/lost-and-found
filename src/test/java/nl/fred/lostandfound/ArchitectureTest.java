package nl.fred.lostandfound;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "nl.fred.lostandfound", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule domainMustNotDependOnWeb = noClasses()
      .that().resideInAPackage("..domain..")
      .should().dependOnClassesThat().resideInAPackage("nl.fred.lostandfound.web..")
      .because("the domain layer must stay free of web/DTO concerns, so it can be reused or "
          + "tested without the web layer");

  @ArchTest
  static final ArchRule controllersMustNotAccessRepositoriesDirectly = noClasses()
      .that().resideInAPackage("nl.fred.lostandfound.web.controller..")
      .should().dependOnClassesThat().resideInAPackage("nl.fred.lostandfound.data.repository..")
      .because("controllers must go through a domain service, not touch persistence directly");

}
