indexroute = 'D:\DefinitiveResults\Recommendation\data\facebook\facebook-index.txt';
trainroute = 'D:\DefinitiveResults\Recommendation\data\facebook\facebook-train.txt';
outputdir =  'D:\DefinitiveResults\Recommendation\rec\facebook\Matlab\';

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

    printrec(strcat(outputdir,strcat(num2str(alpha)),'_ht.txt'), ht, index, numUsers, graph);
    printrec(strcat(outputdir,strcat(num2str(alpha)),'_ct.txt'), ct, index, numUsers, graph);
end

for b = 0.1:0.1:1.0
    katz = katzMatrix(graph,numUsers,b);
    filename = strcat(outputdir,strcat(strcat('KATZ_UND_',num2str(b)),'.txt'));
    printrec(filename, katz,index,numUsers, graph);
end

for b = 0.0:0.1:1.0
    for k = 2:5
        lpi = lpiMatrix(graph,numUsers,b,k);
        filename = strcat(outputdir,strcat(strcat(strcat('LPI_UND_',num2str(b)),strcat('_',num2str(k)))),'.txt');
        printrec(filename, lpi,index,numUsers, graph);
    end
end

for phi = 0.0:0.1:1.0
    lhn2 = lhnindex2(graph,numUsers,numEdges,phi);
    filename = strcat(outputdir,strcat(strcat('LHNIndex2_',num2str(phi)),'.txt'));
    printrec(filename, lhn2, index, numUsers, graph);
end

pic = pseudoinversecosine(graph,numUsers);
filename = strcat(outputdir,'PIC.txt');
printrec(filename, pic, index, numUsers, graph);

for phi = [0.001, 0.01, 0.1, 1, 10, 100, 1000]
    mf = matrixforest(graph,numUsers,phi);
    filename = strcat(outputdir,strcat(strcat('MatrixForest',num2str(phi)),'.txt'));
    printrec(filename, mf, index, numUsers, graph);
end

% Find the restricted hitting and commute times
hitting = hittingtime(graph, numUsers, numEdges);
commute = commutetime(graph, numUsers, numEdges);

printrec(strcat(outputdir,'hittingtime.txt'), hitting, index, numUsers, graph);
printrec(strcat(outputdir,'commutetime.txt'), commute, index, numUsers, graph);
