project
=======

vcluster project

Source code for the vcluster in FermiCloud
Update 7-28-2014

1. BUG for empty plugman command -- fixed
2. VM name for EC2 VMs -- fixed
3. Bugs for empty job queue -- fixed
4. VM overhead model:
   1. Host configuration read -- DONE
   2. Host configuration write -- DONE
   3. Host parameters check -- DONE
   4. Model calculation -- DONE
   5. Overhead aware best fit algorithm
   6. Host benchmarking --DONE
   7. Host constructor --DONE
   8. Model test --DONE
   9. VM release pattern -- need config file
   10. Model traning
   11. Readings from worker nodes 
5. Auto-launch based on model
6. Bugs on proxy server. Read redundant messages from the client side. --FIXED

Update 7-18-2014
Add Functions: Enable list host infomation from private cloud
               Enable getting real time infomation for hosts (CPU Utilization, IO Utilization)
               Match VMs to Hosts, counting running VMs. (Readings from OpenNebula are not accurate)
               
               When load hosts, filters used to select hosts from given cluster
               Enable list condor jobs in details (Owner, Start time, Status, Slot, etc.)

               EC2 feature:
                   Lable worker nodes when instantiate VM on EC2
                   Describe only vCluster worker nodes
               "listhost, hostlist" commands only show hosts informations


