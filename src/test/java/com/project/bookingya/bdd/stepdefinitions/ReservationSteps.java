// ============================================================
// BDD - STEP DEFINITIONS: ReservationSteps
// ------------------------------------------------------------
// Propósito:
// Implementar los pasos de Cucumber que conectan los escenarios
// Gherkin (.feature) con el patrón Screenplay.
//
// Patrón Screenplay aplicado:
// - Actor realiza Tasks (attemptsTo)
// - Actor hace preguntas (should / seeThat)
// - Toda interacción con la API pasa por Tasks encapsuladas
//
// Correcciones sobre el código original:
// 1. Se agrega la Task faltante: ActualizarReserva
// 2. Se corrige ConsultarReserva para no usar "actor" estáticamente
// 3. EliminarReserva ahora guarda lastResponse correctamente
// 4. Se inicializa el actor con CallAnApi (ability de screenplay-rest)
// ============================================================

package com.project.bookingya.bdd.stepdefinitions;

import com.project.bookingya.screenplay.actors.BookingActor;
import com.project.bookingya.screenplay.questions.ResponseStatus;
import com.project.bookingya.screenplay.questions.ReservaExiste;
import com.project.bookingya.screenplay.tasks.*;

import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import net.serenitybdd.screenplay.Actor;

import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ReservationSteps {

    // El actor se comparte entre pasos del mismo escenario
    private Actor actor;

    // GIVEN: CONFIGURACIÓN BASE - huésped y habitación

    @Given("existe un huésped y una habitación disponible")
    public void existeUnHuespedYHabitacion() {

        // Crear actor con habilidades de API
        actor = BookingActor.qaUser();

        // Preparar el contexto: crear guest y room en el sistema
        actor.attemptsTo(
                CrearGuest.enSistema(),
                CrearRoom.disponible()
        );
    }

    // GIVEN: CONFIGURACIÓN CON RESERVA EXISTENTE

    @Given("existe una reserva creada")
    public void existeUnaReservaCreada() {

        // Crear actor con habilidades de API
        actor = BookingActor.qaUser();

        // Preparar el contexto completo: guest + room + reserva
        actor.attemptsTo(
                CrearGuest.enSistema(),
                CrearRoom.disponible(),
                CrearReserva.conDatos()
        );
    }

    // ESCENARIO 1: CREACIÓN DE UNA RESERVA

    @When("creo una reserva válida")
    public void creoReserva() {
        actor.attemptsTo(
                CrearReserva.conDatos()
        );
    }

    @Then("la reserva se crea exitosamente")
    public void validarCreacion() {
        actor.should(
                seeThat("el status HTTP sea 200", ResponseStatus.code(), equalTo(200)),
                seeThat("la reserva tenga ID", ReservaExiste.enSistema(), is(true))
        );
    }

    // ESCENARIO 2: OBTENCIÓN DE UNA RESERVA POR ID

    @When("consulto la reserva por su ID")
    public void consultoPorId() {
        actor.attemptsTo(
                ConsultarReserva.porId()
        );
    }

    @Then("debo obtener la reserva por ID correctamente")
    public void validarGetById() {
        actor.should(
                seeThat("el status HTTP sea 200", ResponseStatus.code(), equalTo(200))
        );
    }

    // ESCENARIO 3: CONSULTA DE UNA RESERVA POR HUÉSPED

    @When("consulto las reservas del huésped")
    public void consultoPorGuest() {
        actor.attemptsTo(
                ConsultarReserva.porGuest()
        );
    }

    @Then("debo obtener las reservas del huésped")
    public void validarConsulta() {
        actor.should(
                seeThat("el status HTTP sea 200", ResponseStatus.code(), equalTo(200))
        );
    }

    // ESCENARIO 4: ACTUALIZACIÓN DE UNA RESERVA EXISTENTE

    @When("actualizo la reserva")
    public void actualizarReserva() {
        actor.attemptsTo(
                ActualizarReserva.conDatos()
        );
    }

    @Then("la reserva debe actualizarse correctamente")
    public void validarUpdate() {
        actor.should(
                seeThat("el status HTTP sea 200", ResponseStatus.code(), equalTo(200))
        );
    }

    // ESCENARIO 5: ELIMINACIÓN DE UNA RESERVA

    @When("elimino la reserva")
    public void eliminarReserva() {
        actor.attemptsTo(
                EliminarReserva.existente()
        );
    }

    @Then("la reserva debe eliminarse correctamente")
    public void validarDelete() {
        actor.should(
                seeThat("el status HTTP sea 200", ResponseStatus.code(), equalTo(200))
        );
    }
}