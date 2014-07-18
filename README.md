project
=======

vcluster project

Source code for the vcluster in FermiCloud

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