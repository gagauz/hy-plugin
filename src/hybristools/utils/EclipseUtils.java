package hybristools.utils;

import java.util.function.BiConsumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class EclipseUtils {
    public static Button createRadio(Composite parent, String label, BiConsumer<Button, SelectionEvent> handler) {
        return createButton(SWT.RADIO, parent, label, handler);
    }

    public static Button createCheckbox(Composite parent, String label, BiConsumer<Button, SelectionEvent> handler) {
        return createButton(SWT.CHECK, parent, label, handler);
    }

    public static Button createButton(Composite parent, String label, BiConsumer<Button, SelectionEvent> handler) {
        return createButton(SWT.BUTTON1, parent, label, handler);
    }

    public static Button createButton(int type, Composite parent, String label, BiConsumer<Button, SelectionEvent> handler) {
        final Button button = new Button(parent, type);
        button.setText(label);
        button.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handler.accept(button, e);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        return button;
    }
}
