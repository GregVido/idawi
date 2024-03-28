package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import toools.collections.Collections;

public class FloodingWithSelfPruning extends RoutingService<SPPParm> {

	public FloodingWithSelfPruning(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "Flooding With Self Pruning";
	}

	@Override
	public String webShortcut() {
		return "fwsp";
	}

	@Override
	public void acceptImpl(Message msg, SPPParm p) {
		// the message was never received
		if (!component.alreadyReceivedMsgs.contains(msg.ID) && !component.alreadySentMsgs.contains(msg.ID)) {
			var myNeighbors = component.outLinks().stream().map(n -> n.dest.component).toList();

			if (msg.route.isEmpty()) {
				component.services(TransportService.class).forEach(t -> t.send(msg, null, this, p));
			} else {
				var srcNeighbors = convert(msg.route.getLast().routing.parms).neighbors;
				var newNeighbors = Collections.difference(myNeighbors, srcNeighbors);

				// if I have neighbors that the source doesn't know
				if (!newNeighbors.isEmpty()) {
					// finds the links to these neighbors
					var links = newNeighbors.stream()
							.flatMap(n -> component.localView().g.findLinksConnecting(component, n).stream()).toList();
					component.services(TransportService.class).forEach(t -> t.send(msg, links, this, p));
				}
			}
		}
	}

	@Override
	public SPPParm defaultParameters() {
		var p = new SPPParm();
		p.neighbors = component.outLinks().stream().map(i -> i.dest.component).toList();
		return p;
	}

	@Override
	public List<SPPParm> dataSuggestions() {
		var l = new ArrayList<SPPParm>();
		l.add(new SPPParm());
		return l;
	}

	@Override
	public ComponentMatcher defaultMatcher(SPPParm parms) {
		return ComponentMatcher.all;
	}

}
