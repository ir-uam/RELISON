/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.ParametersReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.expiration.ExpirationParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.propagation.PropagationParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.selection.SelectionParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.sight.SightParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.update.UpdateParamReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads the grids for several algorithms.
 * @author Javier Sanz-Cruzado Puig
 */
public class ProtocolParamReader extends ParametersReader
{   
    /**
     * Protocol name
     */
    private String name;
    /**
     * In case the protocol is preconfigured, its parameters
     */
    private Parameters params;
    /**
     * In case the protocol is custom, the protocol mechanism
     */
    private SelectionParamReader selection;
    /**
     * In case the protocol is custom, the expiration mechanism
     */
    private ExpirationParamReader expiration;
    /**
     * In case the protocol is custom, the update mechanism
     */
    private UpdateParamReader update;
    /**
     * In case the protocol is custom, the propagation mechanism
     */
    private PropagationParamReader propagation;
    /**
     * In case the protocol is custom, the sight mechanism
     */
    private SightParamReader sight;
    /**
     * Indicates if the protocol is custom or preconfigured
     */
    private boolean preconfigured = false;
    /**
     * Identifier for the name of the protocol
     */
    private static final String NAME = "name";
    /**
     * Identifier for the type of the protocol
     */
    private static final String TYPE = "type";
    /**
     * Identifier for the parameters of the protocol
     */
    private static final String PARAM = "param";
    /**
     * Identifier for the protocol mechanism
     */
    private static final String SELECTION = "selection";
    /**
     * Identifier for the expiration mechanism
     */
    private static final String EXPIRATION = "expiration";
    /**
     * Identifier for the update mechanism
     */
    private static final String UPDATE = "update";
    /**
     * Identifier for the propagation mechanism
     */
    private static final String PROPAGATION = "propagation";
    /**
     * Identifier for the sight mechanism
     */
    private static final String SIGHT = "sight";

    /**
     * Reads a protocol from an XML.
     * @param element the element containing the protocol information.
     */
    public void readProtocol(Element element)
    {
        this.params = null;
        this.selection = null;
        this.expiration = null;
        this.update = null;
        this.propagation = null;
        this.sight = null;
        
        
        this.name = element.getElementsByTagName(NAME).item(0).getTextContent();
        String type = element.getElementsByTagName(TYPE).item(0).getTextContent();
        ProtocolType protType = ProtocolType.valueOf(type);
        
        if(protType.equals(ProtocolType.PRECONFIGURED)) // Read preconfigured algorithms
        {
            NodeList parametersNodes = element.getElementsByTagName(PARAM);
            this.params = this.readParameterGrid(parametersNodes);
            this.preconfigured = true;
        }
        else // Read custom algorithms
        {
            // Read protocol node
            Node selectionNode = element.getElementsByTagName(SELECTION).item(0);
            this.selection = new SelectionParamReader();
            this.selection.readSelection((Element) selectionNode);
            
            // Read expiration node
            Node expirationNode = element.getElementsByTagName(EXPIRATION).item(0);
            this.expiration = new ExpirationParamReader();
            this.expiration.readExpiration((Element) expirationNode);
            
            // Read update node
            Node updateNode = element.getElementsByTagName(UPDATE).item(0);
            this.update = new UpdateParamReader();
            this.update.readUpdate((Element) updateNode);
            
            // Read propagation node
            Node propagationNode = element.getElementsByTagName(PROPAGATION).item(0);
            this.propagation = new PropagationParamReader();
            this.propagation.readPropagation((Element) propagationNode);
            
            // Read sight node
            Node sightNode = element.getElementsByTagName(SIGHT).item(0);
            this.sight = new SightParamReader();
            this.sight.readSight((Element) sightNode);
            
            this.preconfigured = false;
        }
    }

    /**
     * Obtains the name of the protocol.
     * @return the name of the protocol.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Obtains the parameters for the preconfigured protocol.
     * @return the parameters if it is preconfigured, null if not.
     */
    public Parameters getParams() 
    {
        if(this.isPreconfigured())
            return params;
        return null;
    }

    /**
     * Obtains the parameters for the protocol mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public SelectionParamReader getSelection() 
    {
        if(!this.isPreconfigured())
            return selection;
        return null;
    }

    /**
     * Obtains the parameters for the expiration mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public ExpirationParamReader getExpiration() 
    {
        if(!this.isPreconfigured())
            return expiration;
        return null;
    }
    
    /**
     * Obtains the parameters for the update mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public UpdateParamReader getUpdate() 
    {
        if(!this.isPreconfigured())
            return update;
        return null;
    }
    
    /**
     * Obtains the parameters for the propagation mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public PropagationParamReader getPropagation() 
    {
        if(!this.isPreconfigured())
            return propagation;
        return null;    
    }

    /**
     * Obtains the parameters for the sight mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public SightParamReader getSight() 
    {
        if(!this.isPreconfigured())
            return sight;
        return null;
    }
    
    /**
     * Indicates if the protocol is preconfigured or custom.
     * @return true if the protocol is preconfigured, false if it is custom.
     */
    public boolean isPreconfigured()
    {
        return this.preconfigured;
    }

    /**
     * Shows the configuration of a protocol.
     * @return a string containing the configuration of the protocol.
     */
    public String printProtocol() 
    {
        String protocol = "";
        protocol += this.getName() + "\n";

        if(this.isPreconfigured())
        {

            protocol += this.params.getBooleanValues()
                    .entrySet()
                    .stream()
                    .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                    .reduce("", (x,y) -> x + y);

            protocol += this.params.getDoubleValues()
                    .entrySet()
                    .stream()
                    .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                    .reduce("", (x,y) -> x + y);

            protocol += this.params.getIntegerValues()
                    .entrySet()
                    .stream()
                    .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                    .reduce("", (x,y) -> x + y);

            protocol += this.params.getLongValues()
                    .entrySet()
                    .stream()
                    .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                    .reduce("", (x,y) -> x + y);

            protocol += this.params.getStringValues()
                    .entrySet()
                    .stream()
                    .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                    .reduce("", (x,y) -> x + y);

            protocol += this.params.getOrientationValues()
                    .entrySet()
                    .stream()
                    .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                    .reduce("", (x,y) -> x + y);

        }
        else
        {
            protocol += "\tSelection: " + this.selection.printSelectionMechanism() + "\n";
            protocol += "\tExpiration: " + this.expiration.printExpirationMechanism() + "\n";
            protocol += "\tPropagation:" + this.propagation.printPropagationMechanism() + "\n";
            protocol += "\tUpdate: " + this.update.printUpdateMechanism() + "\n";
            protocol += "\tSight: " + this.sight.printSelectionMechanism();
        }
        
        return protocol;

    }

}
