/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.provider.sqrt.ui;

import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import sorcer.core.SorcerConstants;
import sorcer.core.context.PositionalContext;
import sorcer.core.provider.Provider;
import sorcer.provider.sqrt.Sqrt;
import sorcer.service.ContextException;
import sorcer.service.Service;
import sorcer.ui.serviceui.UIComponentFactory;
import sorcer.ui.serviceui.UIDescriptorFactory;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Component Service UI for SORCER Arithmetic - an example service
 */
public class SqrtUI extends JPanel implements SorcerConstants {

	private final static Logger logger = Logger.getLogger(SqrtUI.class
			.getName());

	private ServiceItem item;

	private Service provider;

	private PositionalContext context;

	private JTextField inField;

	private JTextArea outText;

	/** Creates new Sqrt UI Component */
	public SqrtUI(Object obj) {
		super();
		getAccessibleContext().setAccessibleName("Sqrt");
		try {
			item = (ServiceItem) obj;
			logger.info("service class: " + item.service.getClass().getName()
					+ "\nservice object: " + item.service);

			provider = (Provider) item.service;

			// Schedule a job for the event-dispatching thread:
			// creating this application's service UI.
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					createUI();
				}
			});

			// inspect class loader tree
			// com.sun.jini.start.ClassLoaderUtil.displayContextClassLoaderTree();
			// com.sun.jini.start.ClassLoaderUtil.displayClassLoaderTree(provider
			// .getClass().getClassLoader());

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void createUI() {
		setLayout(new BorderLayout());
		JPanel entryPanel = new JPanel(new BorderLayout());
		entryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JLabel lbl = new JLabel(
				"Enter numeric values (space separated):");
		entryPanel.add(lbl, BorderLayout.NORTH);
		inField = new JTextField(40);
		inField.setActionCommand("context");
		inField.addActionListener(new ArithmeticActionListener());
		entryPanel.add(inField, BorderLayout.SOUTH);
		outText = new JTextArea(20, 40);
		JScrollPane scroller = new JScrollPane(outText);
		scroller.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		JButton btn;
		JPanel cmdPanel = new JPanel();
		cmdPanel.setLayout(new BoxLayout(cmdPanel, BoxLayout.X_AXIS));
		cmdPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

		btn = new JButton("Clear");
		btn.setActionCommand("clear");
		btn.addActionListener(new ArithmeticActionListener());
		cmdPanel.add(btn);

		cmdPanel.add(Box.createHorizontalGlue());

		btn = new JButton("Sqrt(x+y)");
		btn.setActionCommand("Sqrt(x+y)");
		btn.addActionListener(new ArithmeticActionListener());
		cmdPanel.add(btn);

		add(entryPanel, BorderLayout.NORTH);
		add(scroller, BorderLayout.CENTER);
		add(cmdPanel, BorderLayout.SOUTH);
	}

	private PositionalContext createServiceContext(String userLine) {
		context = new PositionalContext("sqrt");
		System.out.println("user line: " + userLine);
		String data[] = SorcerUtil.getTokens(userLine, ", ");
		try {
			for (int i = 0; i < data.length; i++) {
				context.putInValueAt("arg/x" + i, new Double(data[i]), i);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ContextException e) {
			e.printStackTrace();
		}
		return context;
	}

	class ArithmeticActionListener implements ActionListener {
		
		public ArithmeticActionListener() {
			super();
		}

		public void actionPerformed(ActionEvent ae) {
			String selector = ae.getActionCommand();
			if (selector.equals("clear")) {
				inField.setText("");
				outText.setText("");
				context = null;
				return;
			} else if (selector.equals("context")) {
				outText.setText("");
				outText.append("Input Data/n");
				context = createServiceContext(inField.getText());
				outText.append(context.toString());
				return;
			}
			try {
				outText.setText("Input Data Context\n");
				outText.append(context.toString());
				outText.append("\n\nOutput Data Context\n");
				outText.append(((Sqrt)provider).add(context).toString());
			} catch (Exception ex) {
				logger.throwing(SqrtUI.class.getName(),
						"actionPerformed", ex);
			}
		}
	}

	/**
	 * Returns a service UI descriptor for this service. Usually this method is
	 * used as an entry in provider configuration files when smart proxies are
	 * deployed with a standard off the shelf {@link sorcer.core.provider.ServiceProvider}.
	 *
	 * @return service UI descriptor
	 */
	public static UIDescriptor getUIDescriptor() {
		UIDescriptor uiDesc = null;
		try {
			String serverVersion = System.getProperty("sorcer.version");
			uiDesc = UIDescriptorFactory.getUIDescriptor(MainUI.ROLE,
					new UIComponentFactory(new URL[] { new URL(Sorcer.getWebsterUrl() + "/sqrt-" + serverVersion + "-ui.jar") },
							SqrtUI.class.getName()));
		} catch (Exception ex) {
			logger.throwing(SqrtUI.class.getName(), "getCalculatorDescriptor", ex);
		}
		return uiDesc;
	}
}
