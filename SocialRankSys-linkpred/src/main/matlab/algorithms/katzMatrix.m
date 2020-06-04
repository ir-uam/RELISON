function katz = katzMatrix(adjMatrix, numUsers, b)
% KATZ Implementation of the Katz similarity algorithm.
%   @args adjMatrix adjacency matrix of the network.
%   @args numUsers number of users in the network.
%   @args b dampening factor.
%
%   @author Javier Sanz-Cruzado
%   @author Pablo Castells
%
    aux = eye(numUsers) - b.*adjMatrix;
    
    katz = inv(aux);
end