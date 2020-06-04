function lpi = lpiMatrix(adjMatrix, numUsers, b, k)
% LPIMATRIX Implementation of the local path index algorithm.
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%   @args b dampening factor
%   @args k maximum distance from the target user
%
%   @author Javier Sanz-Cruzado
%   @author Pablo Castells
%
    aux = adjMatrix;
    beta = 1;
    lpi = zeros(numUsers,numUsers);
    for i = 2:k
        aux = aux * adjMatrix;
        lpi = lpi + beta.*aux;
        beta = beta * b;
    end
end