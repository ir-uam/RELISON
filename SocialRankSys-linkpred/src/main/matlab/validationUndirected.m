%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%

indexroute = 'index route';
trainroute = 'training route';
outputdir =  'output route';

index = importdata(indexroute);
train = importdata(trainroute);

graph = undAdjacencyMatrix(index,train);

numUsers = length(index);
numEdges = sum(sum(graph));
T = zeros(numUsers, numUsers);


for alpha = 0.1:0.1:1.0
    for i = 1:numUsers
        rowsum = sum(graph(i,:));
        if rowsum == 0
            for j=1:numUsers
                T(i,j) = 1.0/numUsers;
            end
        else
            for j=1:numUsers
                T(i,j) = alpha./numUsers + graph(i,j).*(1-alpha)./rowsum;
            end
        end
    end
    
    ht = -1 .* hittingTimeMatrix(T, numUsers);
    ct = ht + transpose(ht);


    printsimplerec(strcat(outputdir,strcat(num2str(alpha)),'_ht.txt'), ht, index, numUsers, graph);
    printsimplerec(strcat(outputdir,strcat(num2str(alpha)),'_ct.txt'), ct, index, numUsers, graph);
end

for b = 0.1:0.1:1.0
    katz = katzMatrix(graph,numUsers,b);
    filename = strcat(outputdir,strcat(strcat('KATZ_UND_',num2str(b)),'.txt'));
    printsimplerec(filename, katz,index,numUsers, graph);
end

for b = 0.0:0.1:1.0
    for k = 2:5
        lpi = lpiMatrix(graph,numUsers,b,k);
        filename = strcat(outputdir,strcat(strcat(strcat('LPI_UND_',num2str(b)),strcat('_',num2str(k)))),'.txt');
        printsimplerec(filename, lpi,index,numUsers, graph);
    end
end

for phi = 0.0:0.1:1.0
    lhn2 = globallhnindex(graph,numUsers,numEdges,phi);
    filename = strcat(outputdir,strcat(strcat('LHNIndex2_',num2str(phi)),'.txt'));
    printsimplerec(filename, lhn2, index, numUsers, graph);
end

pic = pseudoinversecosine(graph,numUsers);
filename = strcat(outputdir,'PIC.txt');
printsimplerec(filename, pic, index, numUsers, graph);

for phi = [0.001, 0.01, 0.1, 1, 10, 100, 1000]
    mf = matrixforest(graph,numUsers,phi);
    filename = strcat(outputdir,strcat(strcat('MatrixForest',num2str(phi)),'.txt'));
    printsimplerec(filename, mf, index, numUsers, graph);
end

% Find the restricted hitting and commute times
hitting = hittingTime(graph, numUsers, numEdges);
commute = commuteTime(graph, numUsers, numEdges);

printsimplerec(strcat(outputdir,'hittingtime.txt'), hitting, index, numUsers, graph);
printsimplerec(strcat(outputdir,'commutetime.txt'), commute, index, numUsers, graph);
