Instalar o Power Tools para poder ver os diagramas do banco 

Criar Web C# .Net MVC

Passo a passo
Criar o projeto
Alterar o nome do projeto
Criar as models
Criar a pasta DAL
Criar o arquivo context
Criar o arquivo initializer (seed)
Ajustar o web.config adicionando o databaseInitializer 
Ou adicionar o Database.SetInitializer no método Application_Start em Global.asax.cs
Configurar o LocalDB no web.config no root do projeto e em views
Para enviar LocalDB junto com o projeto é preciso colocá-lo na pasta App_Data (Adicionar AttachDBFilename=|DataDirectory|\ContosoUniversity.mdf; na connectionString)
Rodar o projeto
Em Controllers -> Add -> New Scaffolded Item -> MVC 5 Controller with views, using Entity Framework.
	Selecionar a Model e o DataContext, alterar o nome para o singular e adicionar
Após isso, dependerá do que deve ser criado em cada página
Criar uma pasta ViewModel
Criar o arquivo EnrollmentDateGroup
Alterar a HomeController incluindo 
	using using ContosoUniversity.DAL; 
	using ContosoUniversity.ViewModels;

	Abaixo a abertura da classe
    	private SchoolContext db = new SchoolContext();

    Substituir o conteúdo do método About
    	IQueryable<EnrollmentDateGroup> data = from student in db.Students
               group student by student.EnrollmentDate into dateGroup
               select new EnrollmentDateGroup()
               {
                   EnrollmentDate = dateGroup.Key,
                   StudentCount = dateGroup.Count()
               };
    	return View(data.ToList());

    Criar o método
    	protected override void Dispose(bool disposing)
		{
		    db.Dispose();
		    base.Dispose(disposing);
		}

	Alterar a View About
		@model IEnumerable<ContosoUniversity.ViewModels.EnrollmentDateGroup>
           
		@{
		    ViewBag.Title = "Student Body Statistics";
		}

		<h2>Student Body Statistics</h2>

		<table>
		    <tr>
		        <th>
		            Enrollment Date
		        </th>
		        <th>
		            Students
		        </th>
		    </tr>

		@foreach (var item in Model) {
		    <tr>
		        <td>
		            @Html.DisplayFor(modelItem => item.EnrollmentDate)
		        </td>
		        <td>
		            @item.StudentCount
		        </td>
		    </tr>
		}
		</table>
Criar a classe de configuração de connection resiliency (repetição de execuções SQL que deram errado por erros de conexão ou algo assim)
	Em DAL, criar SchoolConfiguration.cs
	Nos Controllers troca os catchs com DataException por
		catch (RetryLimitExceededException /* dex */){
		    //Log the error (uncomment dex variable name and add a line here to write a log.
		    ModelState.AddModelError("", "Unable to save changes. Try again, and if the problem persists see your system administrator.");
		}
Criar a classe de logs
	Sempre criar uma classe para que seja mais fácil a manutenção
	Criar a pasta Logging 
		Criar a interface ILogger 
		Criar a classe Logger
		Está sendo utilizado o System.Diagnostics que já vem no EF para tracing, no exemplo somente é feita a impressão no output,
		mas pode ser alterada para gravar em um arquivo ou no banco
		Criar a classe de interceptação para que sejam feitos os logs das transações
	Na pasta DAL 
		Criar SchoolInterceptorLogging.cs que cria Information logs para queries e  comandos. Ou Error log para exceptions
		Criar SchoolInterceptorTransientErrors.cs que será responsável por gerar os erros para teste

	Exemplos de códigos de erros que são transientes (Ocorrem por falhas temporárias e quando executados novamente podem ter sucesso)
		20, 64, 233, 10053, 10054, 10060, 10928, 10929, 40197, 40501, e 40613.
	No Global.asax Adicionar
		using ContosoUniversity.DAL;
		using System.Data.Entity.Infrastructure.Interception;
		E dentro do Application_Start
			DbInterception.Add(new SchoolInterceptorTransientErrors()); -> Testar os erros transientes
    		DbInterception.Add(new SchoolInterceptorLogging()); -> Efetuar o log das transações
    	Também pode ser adicionado diretamente na classe de configuração SchoolConfiguration, o resultado é o mesmo
Habilitar Migrations
	Comentar ou excluir o <contexts> adicionado no Web.config
	Alterar o nome da base de dados para ContosoUniversity2
	Tools -> Library Package Manager -> Package Manager Console e digitar enable-migrations + enter e depois add-migration InitialCreate + enter
	Vai ser criada a pasta Migrations 
	Nela terá o arquivo Configuration que contém o método seed que será utilizado para preencher o banco quando for gerada uma nova migration
	Faz a validação e só adiciona a linha se ela não exitir no banco
		foreach (Enrollment e in enrollments){
		    var enrollmentInDataBase = context.Enrollments.Where(
		        s => s.Student.ID == e.Student.ID &&
		             s.Course.CourseID == e.Course.CourseID).SingleOrDefault();
		    if (enrollmentInDataBase == null){
		        context.Enrollments.Add(e);
		    }
		}
	Digitar o update-database para rodar a migration
	Digitar Update-Database –TargetMigration: TheLastGoodMigration para reverter uma migration

DataAnnotations
	Na Model Student adicionar using System.ComponentModel.DataAnnotations
	Formatar a data para mostrar somente data e não mostrar hora
        [DataType(DataType.Date)]
        [DisplayFormat(DataFormatString = "{0:yyyy-MM-dd}", ApplyFormatInEditMode = true)]
	Fazer as alterações em alunos e gerar a nova migration add-migration MaxLengthOnNames e update-database
	Adicionar using System.ComponentModel.DataAnnotations.Schema e adicionar [Column("FirstName")] em FirstMidName
	Depois criar uma migration add-migration ColumnFirstName

	DataType => Serve para definição de tipo e NÃO serve para validação
	DisplayFormat => Serve para formatação de como será mostrado na tela
	MaxLength => Seta o tamanho mas não provê client-side validação
	StringLength => Seta o tamanho e provê client-side validação 

REGEX
	First character to be upper case and the remaining characters to be alphabetical:
		[RegularExpression(@"^[A-Z]+[a-zA-Z''-'\s]*$")]

FluentAPI
	Serve para gerar as tabelas com as anotações sem precisar das anotações por si
		modelBuilder.Entity<Course>()
	     .HasMany(c => c.Instructors).WithMany(i => i.Courses)
	     .Map(t => t.MapLeftKey("CourseID")
	         .MapRightKey("InstructorID")
	         .ToTable("CourseInstructor"));

	    modelBuilder.Entity<Instructor>()
    		.HasOptional(p => p.OfficeAssignment).WithRequired(p => p.Instructor);


Buscar Single Entity
	Sempre usar o where dentro da chamada para que não seja gerada exceção quando for retornado null 
		.Single(i => i.ID == id.Value) ao invés de .Where(I => i.ID == id.Value).Single()
	Find não permite EagerLoading, para isso usar .Where(i => i.ID == id).Single()


Atualizar EntidadesAssociadas
	public ActionResult EditPost(int? id)
        {
            if (id == null)
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }
            var instructorToUpdate = db.Instructors
               .Include(i => i.OfficeAssignment)
               .Where(i => i.ID == id)
               .Single();

            if (TryUpdateModel(instructorToUpdate, "",
               new string[] { "LastName", "FirstMidName", "HireDate", "OfficeAssignment" }))
            {
                try
                {
                    if (String.IsNullOrWhiteSpace(instructorToUpdate.OfficeAssignment.Location))
                    {
                        instructorToUpdate.OfficeAssignment = null;
                    }

                    db.SaveChanges();

                    return RedirectToAction("Index");
                }
                catch (RetryLimitExceededException /* dex */)
                {
                    //Log the error (uncomment dex variable name and add a line here to write a log.
                    ModelState.AddModelError("", "Unable to save changes. Try again, and if the problem persists, see your system administrator.");
                }
            }
            return View(instructorToUpdate);
        }


Popular dados de entidade associada quando um para muitos
	Para mostrar na na view marcando os selecionados
	    private void PopulateAssignedCourseData(Instructor instructor)
	    {
	        var allCourses = db.Courses;
	        var instructorCourses = new HashSet<int>(instructor.Courses.Select(c => c.CourseID));
	        var viewModel = new List<AssignedCourseData>();
	        foreach (var course in allCourses)
	        {
	            viewModel.Add(new AssignedCourseData
	            {
	                CourseID = course.CourseID,
	                Title = course.Title,
	                Assigned = instructorCourses.Contains(course.CourseID)
	            });
	        }
	        ViewBag.Courses = viewModel;
	    }

	Para atualziar os dados conforme a nova seleção
		private void UpdateInstructorCourses(string[] selectedCourses, Instructor instructorToUpdate)
        {
            if (selectedCourses == null)
            {
                instructorToUpdate.Courses = new List<Course>();
                return;
            }

            var selectedCoursesHS = new HashSet<string>(selectedCourses);
            var instructorCourses = new HashSet<int>
                (instructorToUpdate.Courses.Select(c => c.CourseID));
            foreach (var course in db.Courses)
            {
                if (selectedCoursesHS.Contains(course.CourseID.ToString()))
                {
                    if (!instructorCourses.Contains(course.CourseID))
                    {
                        instructorToUpdate.Courses.Add(course);
                    }
                }
                else
                {
                    if (instructorCourses.Contains(course.CourseID))
                    {
                        instructorToUpdate.Courses.Remove(course);
                    }
                }
            }
        }

    View para mostrar os dados
    	<div class="form-group">
		    <div class="col-md-offset-2 col-md-10">
		        <table>
		            <tr>
		                @{
		                    int cnt = 0;
		                    List<ContosoUniversity.ViewModels.AssignedCourseData> courses = ViewBag.Courses;

		                    foreach (var course in courses)
		                    {
		                        if (cnt++ % 3 == 0)
		                        {
		                            @:</tr><tr>
		                        }
		                        @:<td>
		                            <input type="checkbox"
		                               name="selectedCourses"
		                               value="@course.CourseID"
		                               @(Html.Raw(course.Assigned ? "checked=\"checked\"" : "")) />
		                               @course.CourseID @:  @course.Title
		                        @:</td>
		                    }
		                    @:</tr>
		                }
		        </table>
		    </div>
		</div>
	Vai ser preciso utilizar uma ViewModel
		namespace ContosoUniversity.ViewModels{
		    public class AssignedCourseData{
		        public int CourseID { get; set; }
		        public string Title { get; set; }
		        public bool Assigned { get; set; }
		    }
		}
	Na exclusão
		public ActionResult DeleteConfirmed(int id){
		   Instructor instructor = db.Instructors
		     .Include(i => i.OfficeAssignment)
		     .Where(i => i.ID == id)
		     .Single();

		   db.Instructors.Remove(instructor);

		    var department = db.Departments
		        .Where(d => d.InstructorID == id)
		        .SingleOrDefault();
		    if (department != null){
		        department.InstructorID = null;
		    }

		   db.SaveChanges();
		   return RedirectToAction("Index");
		}


Instalar Pacote
	Tools -> Library -> Package Manager -> Package Manager Console
	Vai abrir o console ter certeza que Package source = nuget.org  e Default project = ContosoUniversity, ou o nome do projeto desejado
	Então é só digitar Install-Package NomeDoPacote => Por Exemplo: Install-Package PagedList.Mvc


Alterar o nome do projeto
	Views\Shared\_Layout.cshtml


Anotações Models
	[DatabaseGenerated(DatabaseGeneratedOption.None)] => Para definir o ID ao invés de deixar ser gerado pelo banco

DAL => Pasta para armazenar os acesso ao banco e os inicializadores de dados
	DatabaseContext => Criado para cada uma das classes que irão ser criadas no banco
		public DbSet<Student> Students { get; set; }
	Seed => Utilizado para preencher o banco,

		protected override void Seed(SchoolContext context)
        {
            var students = new List<Student>
            {
            	new Student{FirstMidName="Carson",LastName="Alexander",EnrollmentDate=DateTime.Parse("2005-09-01")}
            };

            students.ForEach(s => context.Students.Add(s));
            context.SaveChanges();
        }

        Para usar o seed tem que adicionar os campos abaixo no web.config dentro de <entityFramework>
		  <contexts>
		    <context type="ContosoUniversity.DAL.SchoolContext, ContosoUniversity">
		      <databaseInitializer type="ContosoUniversity.DAL.SchoolInitializer, ContosoUniversity" />
		    </context>
		  </contexts>

		Ou adicionar um Database.SetInitializer no método Application_Start em Global.asax.cs


Configurar o Web.config
	
	Se for LocalDB (<2015: Data Source=(LocalDB)\v11.0;) (> 2015: Data Source=(LocalDB)\MSSQLLocalDB;)
		<add name="ConnectionStringName"
	    providerName="System.Data.SqlClient"
	    connectionString="Data Source=(LocalDB)\MSSQLLocalDB;AttachDbFileName=|DataDirectory|\DatabaseFileName.mdf;InitialCatalog=DatabaseName;Integrated Security=True;MultipleActiveResultSets=True" />

	Se for SQL Server Express
		<add name="ConnectionStringName"
	    providerName="System.Data.SqlClient"
	    connectionString="Data Source=.\SQLEXPRESS;AttachDbFileName=|DataDirectory|\DatabaseFileName.mdf;Integrated Security=True;User Instance=True;MultipleActiveResultSets=True" />

	Se for SQL Server Full
		<add name="ConnectionStringName"
	    providerName="System.Data.SqlClient"
	    connectionString="Data Source=ServerName\InstanceName;Initial Catalog=DatabaseName;Integrated Security=True;MultipleActiveResultSets=True" />

	Windows Azure
		<add name="ConnectionStringName"
	    providerName="System.Data.SqlClient"
	    connectionString="Data Source=tcp:ServerName.database.windows.net,1433;Initial Catalog=DatabaseName;Integrated Security=False;User Id=username@servername;Password=password;Encrypt=True;TrustServerCertificate=False;MultipleActiveResultSets=True" />

Conexão com o banco (Exemplo)
	https://msdn.microsoft.com/en-us/data/jj592674
	https://msdn.microsoft.com/en-us/library/jj653752.aspx



ENUM
    public enum Grade
    {
        A, B, C, D, F
    }




Criando uma página
	Utilizar o Bind com include e exclude para permitir ou excluir campos e não permitir inclusão de código malicioso

Página de Index

	Adicionar Ordenação
		Na View
	    <tr>
	        <th>
	            @Html.ActionLink("Last Name", "Index", new { sortOrder = ViewBag.NameSortParm })
	        </th>
	        <th>First Name
	        </th>
	        <th>
	            @Html.ActionLink("Enrollment Date", "Index", new { sortOrder = ViewBag.DateSortParm })
	        </th>
	        <th></th>
	    </tr>

	    No Controller
			public ActionResult Index(string sortOrder)
			{
			   ViewBag.NameSortParm = String.IsNullOrEmpty(sortOrder) ? "name_desc" : "";
			   ViewBag.DateSortParm = sortOrder == "Date" ? "date_desc" : "Date";
			   var students = from s in db.Students
			                  select s;
			   switch (sortOrder)
			   {
			      case "name_desc":
			         students = students.OrderByDescending(s => s.LastName);
			         break;
			      case "Date":
			         students = students.OrderBy(s => s.EnrollmentDate);
			         break;
			      case "date_desc":
			         students = students.OrderByDescending(s => s.EnrollmentDate);
			         break;
			      default:
			         students = students.OrderBy(s => s.LastName);
			         break;
			   }
			   return View(students.ToList());
			}

	Adicionar Ordenadção e Filtro
		Na View
			@using (Html.BeginForm())
			{
			    <p>
			        Find by name: @Html.TextBox("SearchString")  
			        <input type="submit" value="Search" /></p>
			}

		No Controller
	        public ViewResult Index(string sortOrder, string searchString)
	        {
	            ViewBag.NameSortParm = String.IsNullOrEmpty(sortOrder) ? "name_desc" : "";
	            ViewBag.DateSortParm = sortOrder == "Date" ? "date_desc" : "Date";
	            var students = from s in db.Students
	                           select s;
	            if (!String.IsNullOrEmpty(searchString))
	            {
	                students = students.Where(s => s.LastName.Contains(searchString)
	                                       || s.FirstMidName.Contains(searchString));
	            }
	            switch (sortOrder)
	            {
	                case "name_desc":
	                    students = students.OrderByDescending(s => s.LastName);
	                    break;
	                case "Date":
	                    students = students.OrderBy(s => s.EnrollmentDate);
	                    break;
	                case "date_desc":
	                    students = students.OrderByDescending(s => s.EnrollmentDate);
	                    break;
	                default:
	                    students = students.OrderBy(s => s.LastName);
	                    break;
	            }
	            return View(students.ToList());
	        }

	Adicionar Paginação
		Instalar o PagedList.Mvc no NuGet package

		Na View
			@model PagedList.IPagedList<ContosoUniversity.Models.Student>
			@using PagedList.Mvc;
			<link href="~/Content/PagedList.css" rel="stylesheet" type="text/css" />

			@{
			    ViewBag.Title = "Students";
			}

			<h2>Students</h2>

			<p>
			    @Html.ActionLink("Create New", "Create")
			</p>
			@using (Html.BeginForm("Index", "Student", FormMethod.Get))
			{
			    <p>
			        Find by name: @Html.TextBox("SearchString", ViewBag.CurrentFilter as string)
			        <input type="submit" value="Search" />
			    </p>
			}
			<table class="table">
			    <tr>
			        <th>
			            @Html.ActionLink("Last Name", "Index", new { sortOrder = ViewBag.NameSortParm, currentFilter=ViewBag.CurrentFilter })
			        </th>
			        <th>
			            First Name
			        </th>
			        <th>
			            @Html.ActionLink("Enrollment Date", "Index", new { sortOrder = ViewBag.DateSortParm, currentFilter=ViewBag.CurrentFilter })
			        </th>
			        <th></th>
			    </tr>

			@foreach (var item in Model) {
			    <tr>
			        <td>
			            @Html.DisplayFor(modelItem => item.LastName)
			        </td>
			        <td>
			            @Html.DisplayFor(modelItem => item.FirstMidName)
			        </td>
			        <td>
			            @Html.DisplayFor(modelItem => item.EnrollmentDate)
			        </td>
			        <td>
			            @Html.ActionLink("Edit", "Edit", new { id=item.ID }) |
			            @Html.ActionLink("Details", "Details", new { id=item.ID }) |
			            @Html.ActionLink("Delete", "Delete", new { id=item.ID })
			        </td>
			    </tr>
			}

			</table>
			<br />
			Page @(Model.PageCount < Model.PageNumber ? 0 : Model.PageNumber) of @Model.PageCount

			@Html.PagedListPager(Model, page => Url.Action("Index", 
			    new { page, sortOrder = ViewBag.CurrentSort, currentFilter = ViewBag.CurrentFilter }))


		No Controller
			Adicionar
				using PagedList;
			Alterar o index 
				public ViewResult Index(string sortOrder, string currentFilter, string searchString, int? page)
				{
				   ViewBag.CurrentSort = sortOrder;
				   ViewBag.NameSortParm = String.IsNullOrEmpty(sortOrder) ? "name_desc" : "";
				   ViewBag.DateSortParm = sortOrder == "Date" ? "date_desc" : "Date";

				   if (searchString != null)
				   {
				      page = 1;
				   }
				   else
				   {
				      searchString = currentFilter;
				   }

				   ViewBag.CurrentFilter = searchString;

				   var students = from s in db.Students
				                  select s;
				   if (!String.IsNullOrEmpty(searchString))
				   {
				      students = students.Where(s => s.LastName.Contains(searchString)
				                             || s.FirstMidName.Contains(searchString));
				   }
				   switch (sortOrder)
				   {
				      case "name_desc":
				         students = students.OrderByDescending(s => s.LastName);
				         break;
				      case "Date":
				         students = students.OrderBy(s => s.EnrollmentDate);
				         break;
				      case "date_desc":
				         students = students.OrderByDescending(s => s.EnrollmentDate);
				         break;
				      default:  // Name ascending 
				         students = students.OrderBy(s => s.LastName);
				         break;
				   }

				   int pageSize = 3;
				   int pageNumber = (page ?? 1);
				   return View(students.ToPagedList(pageNumber, pageSize));
				}

	Dica:
		The default BeginForm submits form data with a POST, which means that parameters are passed in the HTTP message 
		body and not in the URL as query strings. When you specify HTTP GET, the form data is passed in the URL as query 
		strings, which enables users to bookmark the URL. 
		The W3C guidelines for the use of HTTP GET recommend that you should use GET when the action does not result in an update.

Tratamento de Concorrência
	É preciso atualizar o projeto para que o Entity Framework possa detectar estas mudanças. 
	Adicionar uma coluna para tracking para determinar quando a tabela for alterada. 
	A coluna RowVersion deve ser incrementada cada vez que a tabela for alterada.
	Adicionar ao model
	    [Timestamp]
	    public byte[] RowVersion { get; set; }

	    ou usando FluentAPI
	    	modelBuilder.Entity<Department>()
	    	.Property(p => p.RowVersion).IsConcurrencyToken();
	Adicionar ao Controller no método EditPost
		[HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> Edit(int? id, byte[] rowVersion)
        {
            string[] fieldsToBind = new string[] { "Name", "Budget", "StartDate", "InstructorID", "RowVersion" };

            if (id == null)
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }

            var departmentToUpdate = await db.Departments.FindAsync(id);
            if (departmentToUpdate == null)
            {
                Department deletedDepartment = new Department();
                TryUpdateModel(deletedDepartment, fieldsToBind);
                ModelState.AddModelError(string.Empty,
                    "Unable to save changes. The department was deleted by another user.");
                ViewBag.InstructorID = new SelectList(db.Instructors, "ID", "FullName", deletedDepartment.InstructorID);
                return View(deletedDepartment);
            }

            if (TryUpdateModel(departmentToUpdate, fieldsToBind))
            {
                try
                {
                    db.Entry(departmentToUpdate).OriginalValues["RowVersion"] = rowVersion;
                    await db.SaveChangesAsync();

                    return RedirectToAction("Index");
                }
                catch (DbUpdateConcurrencyException ex)
                {
                    var entry = ex.Entries.Single();
                    var clientValues = (Department)entry.Entity;
                    var databaseEntry = entry.GetDatabaseValues();
                    if (databaseEntry == null)
                    {
                        ModelState.AddModelError(string.Empty,
                            "Unable to save changes. The department was deleted by another user.");
                    }
                    else
                    {
                        var databaseValues = (Department)databaseEntry.ToObject();

                        if (databaseValues.Name != clientValues.Name)
                            ModelState.AddModelError("Name", "Current value: "
                                + databaseValues.Name);
                        if (databaseValues.Budget != clientValues.Budget)
                            ModelState.AddModelError("Budget", "Current value: "
                                + String.Format("{0:c}", databaseValues.Budget));
                        if (databaseValues.StartDate != clientValues.StartDate)
                            ModelState.AddModelError("StartDate", "Current value: "
                                + String.Format("{0:d}", databaseValues.StartDate));
                        if (databaseValues.InstructorID != clientValues.InstructorID)
                            ModelState.AddModelError("InstructorID", "Current value: "
                                + db.Instructors.Find(databaseValues.InstructorID).FullName);
                        ModelState.AddModelError(string.Empty, "The record you attempted to edit "
                            + "was modified by another user after you got the original value. The "
                            + "edit operation was canceled and the current values in the database "
                            + "have been displayed. If you still want to edit this record, click "
                            + "the Save button again. Otherwise click the Back to List hyperlink.");
                        departmentToUpdate.RowVersion = databaseValues.RowVersion;
                    }
                }
                catch (RetryLimitExceededException /* dex */)
                {
                    //Log the error (uncomment dex variable name and add a line here to write a log.
                    ModelState.AddModelError("", "Unable to save changes. Try again, and if the problem persists, see your system administrator.");
                }
            }
            ViewBag.InstructorID = new SelectList(db.Instructors, "ID", "FullName", departmentToUpdate.InstructorID);
            return View(departmentToUpdate);
        }
    Se não for preciso mostrar os valores atuais dos campos, não é preciso criar a entidade temporária
    Se o FindAsync retornar null é porque a entidade for deletada por outro usuário
    Ao tentar atualizar um arquivo o na cláusula where será passado o antigo RowVersion
    Se nenhuma row for retornada será gerada a exceção DbUpdateConcurrencyException
    Na exceção usando var entry = ex.Entries.Single(); podem ser acessados os valores atuais da entidade 
    Usando var clientValues = (Department)entry.Entity; e var databaseEntry = entry.GetDatabaseValues(); é criada uma entidade temporária com os dados
    O valor de RowVersion deve ser atualizado para o novo valor 
    A página de edição deve ser mostrada com os valores antigos e com as mensagens dos valores novos e a mensagem de porque não foi atualizado
    A página deve armazenar o valor de RowVersion em um hiddenField


    No Delete
    	public async Task<ActionResult> Delete(int? id, bool? concurrencyError)
        {
            if (id == null)
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }
            Department department = await db.Departments.FindAsync(id);
            if (department == null)
            {
                if (concurrencyError.GetValueOrDefault())
                {
                    return RedirectToAction("Index");
                }
                return HttpNotFound();
            }

            if (concurrencyError.GetValueOrDefault())
            {
                ViewBag.ConcurrencyErrorMessage = "The record you attempted to delete "
                    + "was modified by another user after you got the original values. "
                    + "The delete operation was canceled and the current values in the "
                    + "database have been displayed. If you still want to delete this "
                    + "record, click the Delete button again. Otherwise "
                    + "click the Back to List hyperlink.";
            }

            return View(department);
        }

        // POST: Department/Delete/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> Delete(Department department)
        {
            try
            {
                db.Entry(department).State = EntityState.Deleted;
                await db.SaveChangesAsync();
                return RedirectToAction("Index");
            }
            catch (DbUpdateConcurrencyException)
            {
                return RedirectToAction("Delete", new { concurrencyError = true, id = department.DepartmentID });
            }
            catch (DataException /* dex */)
            {
                //Log the error (uncomment dex variable name after DataException and add a line here to write a log.
                ModelState.AddModelError(string.Empty, "Unable to delete. Try again, and if the problem persists contact your system administrator.");
                return View(department);
            }
        }


	Configurar o EF para incluir esta coluna em cláusulas de update e delete
	O Update e Delete deverá conter o valor original, quando o EF perceber que nenhuma row foi atualizada ele verá que ocorreu um conflito.
	

	Manter todos os itens armazenados
		Não e muito utilizado, pois mantém muitos dados
		Configurar o Ef para incluir o valor original de todos os campos nas queries de Update e Delete.
		Adicionar ConcurrencyCheck em todas as colunas que devem ser trakeadas. 

	Concorrência Pessimista
		Lock
			Read-Only Access -> Todos podem ler, mas só quem solicitou o lock pode alterar

			Update -> Ninguém pode ler, somente quem solicitou o lock, o qual pode ler e escrever

	Concorrência Otimista
		Client-Wins -> O que atualziar por último fica gravado no banco
		Server-Wins -> O que está no servidor prevalece


Herança
	TPH -> Table-Per-Hierarchy 
	TCT -> Table-Per-Type
	TCP -> Table-Per-Concrete Class

	TCP e TPH normalmente funcionam melhor com EF, pois TCT normalmente resulta em joins complexos e incompletos
	TPH é o padrão para o EF


Página de Create ou Edit
	@Html.AntiForgeryToken() -> Usado em conjunto com [ValidateAntiForgeryToken] no controller para evitar ataques 
	@Html.EditorFor(model => model.LastName)
    @Html.ValidationMessageFor(model => model.LastName)


Página de Edit
	Na View
	@Html.AntiForgeryToken() -> Usado em conjunto com [ValidateAntiForgeryToken] no controller para evitar ataques 
	@Html.EditorFor(model => model.LastName)
    @Html.ValidationMessageFor(model => model.LastName)

    No Controller 
    Retirar o bind do EditPost, pois ele limpa os campos que não foram alterados
    Utilizar o TryUpdateModel passando os campos que devem ser alterados. Todos que não estiverem na lista serão ignorados


Página de Detalhes
	<dt>
        @Html.DisplayNameFor(model => model.EnrollmentDate)
    </dt>

    <dd>
        @Html.DisplayFor(model => model.EnrollmentDate)
    </dd>

Página de Delete
	Na View
	Adicionar a linha abaixo para mostrar a mensagem de erro caso seja gerada 
	<p class="error">@ViewBag.ErrorMessage</p>

	No Controller
	Adicionar os dados abaixo para permitir que caso ocorra um erro na deleção seja mostrada a pagina de delete com a mensagem informativa 
	// GET: Student/Delete/5
        public ActionResult Delete(int? id, bool? saveChangesError = false)
        {
            if (id == null)
            {
                return new HttpStatusCodeResult(HttpStatusCode.BadRequest);
            }
            if (saveChangesError.GetValueOrDefault())
            {
                ViewBag.ErrorMessage = "Delete failed. Try again, and if the problem persists see your system administrator.";
            }
            Student student = db.Students.Find(id);
            if (student == null)
            {
                return HttpNotFound();
            }
            return View(student);
        }

        // POST: Student/Delete/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult Delete(int id)
        {
            try
            {
                Student student = db.Students.Find(id);
                db.Students.Remove(student);
                db.SaveChanges();
            }
            catch (DataException/* dex */)
            {
                //Log the error (uncomment dex variable name and add a line here to write a log.
                return RedirectToAction("Delete", new { id = id, saveChangesError = true });
            }
            return RedirectToAction("Index");
        }


