package es.uam.eps.socialranksys.diffusion.metrics.features.global;

import es.uam.eps.socialranksys.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class representing individual feature-based metrics which do not take into account features that the user already knows
 * (with already knows meaning that the user has the feature, in case of user features, or the user has an information piece containing
 * the feature, in case of information features).
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public abstract class AbstractExternalFeatureGlobalSimulationMetric<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalSimulationMetric<U,I,P>
{
    /**
     * The set of own parameters for each user.
     */
    private final Map<U, Set<P>> ownParams;

    /**
     * Constructor.
     * @param name Name of the metric.
     * @param parameter Name of the metric parameter.
     * @param userparam True if the parameter is a user parameter, false if it is not.
     */
    public AbstractExternalFeatureGlobalSimulationMetric(String name, String parameter, boolean userparam)
    {
        super(name, userparam, parameter);
        this.ownParams = new HashMap<>();
    }

    /**
     * Obtains the map identifying the parameters of all users.
     * @return the parameters of all users.
     */
    protected Map<U, Set<P>> getOwnParams()
    {
        return this.ownParams;
    }

    /**
     * Obtains the parameters that an individual user already knows.
     * @param u the user
     * @return the set of parameters the user already knows. If the user does not exist, an empty set is returned.
     */
    protected Set<P> getOwnParams(U u)
    {
        return this.ownParams.getOrDefault(u, new HashSet<>());
    }

    /**
     * Computes and stores the own params for every user in the network.
     */
    protected void computeOwnParams()
    {
        this.data.getAllUsers().forEach(u ->
        {
            Set<P> userParams = this.computeOwnParams(u);
            this.ownParams.put(u, userParams);
        });
    }

    /**
     * Computes the parameters for a user
     * @param u the user.
     * @return the parameter set for the user
     */
    protected Set<P> computeOwnParams(U u)
    {
        if(this.usesUserParam())
        {
            return this.computeOwnUserParams(u);
        }
        else
        {
            return this.computeOwnInfoParams(u);
        }
    }

    /**
     * Computes the user parameters for an individual user.
     * @param u the user
     * @return the parameter set.
     */
    protected Set<P> computeOwnUserParams(U u)
    {
        Set<P> parameters = new HashSet<>();

        data.getUserFeatures(u, this.getParameter()).forEach(p -> parameters.add(p.v1));

        return parameters;
    }

    /**
     * Computes information piece parameters for an individual user.
     * @param u the user
     * @return the parameter set.
     */
    protected Set<P> computeOwnInfoParams(U u)
    {
        Set<P> parameters = new HashSet<>();

        data.getPieces(u).forEach(i ->
            data.getInfoPiecesFeatures(i, this.getParameter()).forEach(p ->
                parameters.add(p.v1)
            )
        );

        return parameters;
    }

    protected void clearOwnParams()
    {
        this.ownParams.clear();
    }

    /**
     * Adds params for an individual user.
     * @param u the user
     * @param params the parameters.
     */
    protected void setOwnParams(U u, Set<P> params)
    {
        this.ownParams.put(u, params);
    }
}
