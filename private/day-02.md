today(14-10-2026), today i only sent prompt to claude code to organize the architecture and documentations in the repo, then i made the obsidian docs more intuitive, with more images, colors and diagrams.
i will spend the remaining time reading the intuitively designed architecture,  and adr files then complete the journal and the own it work.
i designed a canvas representing the relationship between each module in the Spring Boot Project.
the relationships are devised into to groups, Read and Event.
Each time a module needs last updated number from other module, he should read it.
Each time there is an Event triggering other module's update, then the relationship is Event.
Example : 
	when User register, then there is an Event Called UserRegister triggering the Profile module.
	 and when the User want to see his Insights(balance) then the module Insights would Read the last numbers from Ledger(the Diary)
Question :
	 how can i understand what is an API concretely?