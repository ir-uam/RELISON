function lhn2 = globallhnindex(adjMatrix, numUsers, numEdges, phi)
% GLOBALLHNINDEX Implementation of the global Leicht-Holme-Newman algorithm
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%   @args numEdges number of edges in the network
%   @args phi dampening factor
%
%   @author Javier Sanz-Cruzado
%   @author Pablo Castells
%
    D = inv(diag(sum(adjMatrix)));
    lambda1 = max(eig(adjMatrix));
    
    X = eye(numUsers) - (phi./lambda1).*adjMatrix;
    lhn2 = 2.*numEdges.*lambda1.*(D*inv(X)*D);
end