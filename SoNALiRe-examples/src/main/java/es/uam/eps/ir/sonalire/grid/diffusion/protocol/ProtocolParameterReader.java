/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.protocol;

import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.ParametersReader;
import es.uam.eps.ir.sonalire.grid.diffusion.expiration.ExpirationParameterReader;
import es.uam.eps.ir.sonalire.grid.diffusion.propagation.PropagationParameterReader;
import es.uam.eps.ir.sonalire.grid.diffusion.selection.SelectionParameterReader;
import es.uam.eps.ir.sonalire.grid.diffusion.sight.SightParameterReader;
import es.uam.eps.ir.sonalire.grid.diffusion.update.UpdateParameterReader;

import java.util.Map;

/**
 * Reads the parameters for diffusion protocols.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ProtocolParameterReader extends ParametersReader
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
    private SelectionParameterReader selection;
    /**
     * In case the protocol is custom, the expiration mechanism
     */
    private ExpirationParameterReader expiration;
    /**
     * In case the protocol is custom, the update mechanism
     */
    private UpdateParameterReader update;
    /**
     * In case the protocol is custom, the propagation mechanism
     */
    private PropagationParameterReader propagation;
    /**
     * In case the protocol is custom, the sight mechanism
     */
    private SightParameterReader sight;
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
    private static final String PARAMS = "params";
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
     * @param protocol the element containing the protocol information.
     */
    public void readProtocol(Map<String, Object> protocol)
    {
        this.params = null;
        this.selection = null;
        this.expiration = null;
        this.update = null;
        this.propagation = null;
        this.sight = null;
        
        this.name = protocol.get(NAME).toString();
        String type = protocol.get(TYPE).toString();
        ProtocolType protType = ProtocolType.valueOf(type);

        if(protType.equals(ProtocolType.PRECONFIGURED)) // Read preconfigured algorithms
        {
            if(!protocol.containsKey(PARAMS) || protocol.get(PARAMS).getClass() == String.class)
            {
                this.params = new Parameters();
            }
            else
            {
                this.params = this.readParameterValues((Map<String, Object>) protocol.get(PARAMS));
            }
            this.preconfigured = true;
        }
        else // Read custom algorithms
        {
            // Read the selection mechanism
            Map<String, Object> map = (Map<String, Object>) protocol.get(SELECTION);
            this.selection = new SelectionParameterReader();
            selection.readSelection(map);

            // Read the expiration mechanism
            map = (Map<String, Object>) protocol.get(EXPIRATION);
            this.expiration = new ExpirationParameterReader();
            expiration.readExpiration(map);

            // Read the update mechanism
            map = (Map<String, Object>) protocol.get(UPDATE);
            this.update = new UpdateParameterReader();
            update.readUpdate(map);

            // Read the expiration mechanism
            map = (Map<String, Object>) protocol.get(PROPAGATION);
            this.propagation = new PropagationParameterReader();
            propagation.readPropagation(map);

            // Read the expiration mechanism
            map = (Map<String, Object>) protocol.get(SIGHT);
            this.sight = new SightParameterReader();
            sight.readSight(map);

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
    public SelectionParameterReader getSelection()
    {
        if(!this.isPreconfigured())
            return selection;
        return null;
    }

    /**
     * Obtains the parameters for the expiration mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public ExpirationParameterReader getExpiration()
    {
        if(!this.isPreconfigured())
            return expiration;
        return null;
    }
    
    /**
     * Obtains the parameters for the update mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public UpdateParameterReader getUpdate()
    {
        if(!this.isPreconfigured())
            return update;
        return null;
    }
    
    /**
     * Obtains the parameters for the propagation mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public PropagationParameterReader getPropagation()
    {
        if(!this.isPreconfigured())
            return propagation;
        return null;    
    }

    /**
     * Obtains the parameters for the sight mechanism.
     * @return the parameters if it is not preconfigured, null if not.
     */
    public SightParameterReader getSight()
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
            protocol += "\tSight: " + this.sight.printSightMechanism();
        }
        
        return protocol;

    }

}
