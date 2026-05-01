// ────────────────────────────────────────────────────────────────────
// BDD - RUNNER DE EJECUCIÓN CON CUCUMBER + SERENITY
// ────────────────────────────────────────────────────────────────────
// Objetivo:
// Ejecutar los escenarios definidos en el archivo reservation.feature
// utilizando Cucumber y generar reportes con Serenity.
//
// Enfoque:
// - Se utiliza Cucumber para definir escenarios en lenguaje Gherkin
// - Se utiliza Serenity para la ejecución y generación de reportes
//
// Flujo de ejecución:
// Runner → Feature → StepDefinitions
// ────────────────────────────────────────────────────────────────────

package com.project.bookingya.bdd.runners;

import org.junit.runner.RunWith;
import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;

// ANOTACIÓN DE EJECUCIÓN
// Indica que las pruebas se ejecutan con Cucumber integrado con Serenity
@RunWith(CucumberWithSerenity.class)

// CONFIGURACIÓN DE CUCUMBER
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.project.bookingya.bdd.stepdefinitions",
        snippets = CucumberOptions.SnippetType.CAMELCASE,
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json"
        }
)
public class ReservationRunnerTest {
    // Clase runner: no contiene lógica
    // Solo actúa como punto de entrada para los escenarios BDD
}
