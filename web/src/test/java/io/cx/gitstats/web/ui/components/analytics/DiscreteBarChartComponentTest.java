package io.cx.gitstats.web.ui.components.analytics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscreteBarChartComponentTest {

    @Test
    void discreteBarChartComponent_whenItemsNull_thenEmptyTextShown() {
        // given
        List<DiscreteBarChartComponent.BarItem> items = null;

        // when
        var component = new DiscreteBarChartComponent(items, "No items available");

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void discreteBarChartComponent_whenItemsEmpty_thenDefaultEmptyTextShown() {
        // given
        List<DiscreteBarChartComponent.BarItem> items = List.of();

        // when
        var component = new DiscreteBarChartComponent(items, null);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void discreteBarChartComponent_whenValidItems_thenRowsConstructed() {
        // given
        var items = List.of(
                new DiscreteBarChartComponent.BarItem("Label 1", 50.0, "10 (50%)"),
                new DiscreteBarChartComponent.BarItem("Label 2", 80.0, "25"),
                new DiscreteBarChartComponent.BarItem("Label 3", 0.0, "0")
        );

        // when
        var component = new DiscreteBarChartComponent(items, "No data");

        // then
        assertThat(component.getChildren()).hasSize(3);
    }
}
