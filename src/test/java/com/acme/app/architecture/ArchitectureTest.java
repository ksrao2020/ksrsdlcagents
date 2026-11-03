package com.acme.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import com.acme.app.shared.domain.exception.DomainException;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/**
 * Executable version of docs/architecture/ARCHITECTURE.md. These rules apply to every bounded context, so new
 * user stories are checked automatically. Never weaken or @Disable a rule to make a story pass.
 */
@AnalyzeClasses(packages = "com.acme.app", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_dependencies_point_inwards = onionArchitecture()
            .domainModels("..domain..")
            .applicationServices("..application..")
            .adapter("web", "..adapter.in..")
            .adapter("persistence", "..adapter.out..")
            .withOptionalLayers(true);

    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.validation..",
                    "com.fasterxml.jackson..", "com.acme.app.api..", "org.hibernate..")
            .because("the domain must stay pure Java (Lombok is allowed)");

    @ArchTest
    static final ArchRule application_does_not_know_api_models_or_jpa = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("com.acme.app.api..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule generated_api_models_stay_in_web_adapter = noClasses()
            .that().resideOutsideOfPackages("..adapter.in.web..", "com.acme.app.api..")
            .should().dependOnClassesThat().resideInAPackage("com.acme.app.api..");

    @ArchTest
    static final ArchRule entities_live_in_persistence_adapter = classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..adapter.out.persistence..")
            .andShould().haveSimpleNameEndingWith("Entity");

    @ArchTest
    static final ArchRule spring_data_repositories_live_in_persistence_adapter = classes()
            .that().areAssignableTo(Repository.class)
            .should().resideInAPackage("..adapter.out.persistence..")
            .andShould().notBePublic();

    @ArchTest
    static final ArchRule transactions_only_on_application_services = classes()
            .that().areAnnotatedWith(Transactional.class)
            .should().resideInAPackage("..application.service..")
            .because("transaction boundaries belong to use cases, not controllers or adapters");

    @ArchTest
    static final ArchRule no_method_level_transactions_outside_services = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..application.service..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_implement_generated_openapi_interfaces = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..adapter.in.web..")
            .andShould().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                    "an OpenAPI-generated interface",
                    javaClass -> javaClass.getPackageName().equals("com.acme.app.api")))
            .because("the API contract is generated from api.yaml (contract-first)");

    @ArchTest
    static final ArchRule mappers_are_mapstruct = classes()
            .that().haveSimpleNameEndingWith("Mapper").and().areInterfaces()
            .should().beAnnotatedWith(Mapper.class);

    @ArchTest
    static final ArchRule no_reflection_based_mapping = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.beans.BeanUtils")
            .orShould().dependOnClassesThat().resideInAnyPackage("org.modelmapper..", "org.dozer..")
            .because("all object conversion goes through MapStruct");

    @ArchTest
    static final ArchRule domain_exceptions_extend_base = classes()
            .that().resideInAPackage("..domain.exception..")
            .should().beAssignableTo(DomainException.class);

    @ArchTest
    static final ArchRule constructor_injection_only = noFields().should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule use_cases_are_interfaces = classes()
            .that().resideInAPackage("..application.port..")
            .should().beInterfaces();
}
