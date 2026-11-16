package io.cx.gitstats.web.ui.utils;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UiUtils {

    public static final String DEFAULT_PAGE_WIDTH = "900px";
    private static final int DEFAULT_DURATION = 3000;
    private static final Notification.Position DEFAULT_POSITION = Notification.Position.TOP_END;

    public static void showSuccessNotification(String message) {
        Notification.show(message, DEFAULT_DURATION, DEFAULT_POSITION)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public static void showErrorNotification(String message) {
        Notification.show(message, DEFAULT_DURATION, DEFAULT_POSITION)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
