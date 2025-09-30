/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service.form.issue_management;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.issue_management.CreateIssueCommentRequest;
import org.spin.backend.grpc.issue_management.CreateIssueRequest;
import org.spin.backend.grpc.issue_management.DeleteIssueCommentRequest;
import org.spin.backend.grpc.issue_management.DeleteIssueRequest;
import org.spin.backend.grpc.issue_management.ExistsIssuesRequest;
import org.spin.backend.grpc.issue_management.ExistsIssuesResponse;
import org.spin.backend.grpc.issue_management.Issue;
import org.spin.backend.grpc.issue_management.IssueComment;
import org.spin.backend.grpc.issue_management.IssueManagementGrpc.IssueManagementImplBase;
import org.spin.backend.grpc.issue_management.ListBusinessPartnersRequest;
import org.spin.backend.grpc.issue_management.ListBusinessPartnersResponse;
import org.spin.backend.grpc.issue_management.ListCategoriesRequest;
import org.spin.backend.grpc.issue_management.ListCategoriesResponse;
import org.spin.backend.grpc.issue_management.ListGroupsRequest;
import org.spin.backend.grpc.issue_management.ListGroupsResponse;
import org.spin.backend.grpc.issue_management.ListIssueCommentsReponse;
import org.spin.backend.grpc.issue_management.ListIssueCommentsRequest;
import org.spin.backend.grpc.issue_management.ListIssuesReponse;
import org.spin.backend.grpc.issue_management.ListIssuesRequest;
import org.spin.backend.grpc.issue_management.ListPrioritiesRequest;
import org.spin.backend.grpc.issue_management.ListPrioritiesResponse;
import org.spin.backend.grpc.issue_management.ListProjectsRequest;
import org.spin.backend.grpc.issue_management.ListProjectsResponse;
import org.spin.backend.grpc.issue_management.ListRequestTypesRequest;
import org.spin.backend.grpc.issue_management.ListRequestTypesResponse;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesRequest;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesResponse;
import org.spin.backend.grpc.issue_management.ListStatusCategoriesRequest;
import org.spin.backend.grpc.issue_management.ListStatusCategoriesResponse;
import org.spin.backend.grpc.issue_management.ListStatusesRequest;
import org.spin.backend.grpc.issue_management.ListStatusesResponse;
import org.spin.backend.grpc.issue_management.ListTaskStatusesRequest;
import org.spin.backend.grpc.issue_management.ListTaskStatusesResponse;
import org.spin.backend.grpc.issue_management.UpdateIssueCommentRequest;
import org.spin.backend.grpc.issue_management.UpdateIssueRequest;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class IssueManagementService extends IssueManagementImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(IssueManagementService.class);


	/**
	 * get: "/issue-management/request-types"
	 */
	@Override
	public void listRequestTypes(ListRequestTypesRequest request, StreamObserver<ListRequestTypesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListRequestTypesResponse.Builder entityValueList = IssueManagementLogic.listRequestTypes(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/sales-representatives"
	 */
	@Override
	public void listSalesRepresentatives(ListSalesRepresentativesRequest request, StreamObserver<ListSalesRepresentativesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListSalesRepresentativesResponse.Builder entityValueList = IssueManagementLogic.listSalesRepresentatives(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/priorities"
	 */
	@Override
	public void listPriorities(ListPrioritiesRequest request, StreamObserver<ListPrioritiesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListPrioritiesResponse.Builder entityValueList = IssueManagementLogic.listPriorities(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/status-categories"
	 */
	@Override
	public void listStatusCategories(ListStatusCategoriesRequest request, StreamObserver<ListStatusCategoriesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListStatusCategoriesResponse.Builder entityValueList = IssueManagementLogic.listStatusCategories(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/statuses"
	 * get: "/issue-management/statuses/types/{request_type_id}"
	 * get: "/issue-management/statuses/categories/{status_category_id}"
	 */
	@Override
	public void listStatuses(ListStatusesRequest request, StreamObserver<ListStatusesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListStatusesResponse.Builder entityValueList = IssueManagementLogic.listStatuses(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/categories"
	 */
	@Override
	public void listCategories(ListCategoriesRequest request, StreamObserver<ListCategoriesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListCategoriesResponse.Builder entityValueList = IssueManagementLogic.listCategories(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/groups"
	 */
	@Override
	public void listGroups(ListGroupsRequest request, StreamObserver<ListGroupsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListGroupsResponse.Builder entityValueList = IssueManagementLogic.listGroups(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/task-statuses"
	 */
	@Override
	public void listTaskStatuses(ListTaskStatusesRequest request, StreamObserver<ListTaskStatusesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListTaskStatusesResponse.Builder entityValueList = IssueManagementLogic.listTaskStatuses(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/business-partners"
	 */
	@Override
	public void listBusinessPartners(ListBusinessPartnersRequest request, StreamObserver<ListBusinessPartnersResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListBusinessPartnersResponse.Builder entityValueList = IssueManagementLogic.listBusinessPartners(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/projects"
	 */
	@Override
	public void listProjects(ListProjectsRequest request, StreamObserver<ListProjectsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListProjectsResponse.Builder entityValueList = IssueManagementLogic.listProjects(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/issues/{table_name}/{record_id}/exists"
	 */
	@Override
	public void existsIssues(ExistsIssuesRequest request, StreamObserver<ExistsIssuesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ExistsIssuesResponse.Builder entityValueList = IssueManagementLogic.existsIssues(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/issues/all"
	 */
	@Override
	public void listIssues(ListIssuesRequest request, StreamObserver<ListIssuesReponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("List Issues Requested is Null");
			}
			ListIssuesReponse.Builder builderList = IssueManagementLogic.listIssues(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/issue-management/issues"
	 * get: "/issue-management/issues/{table_name}/{record_id}"
	 */
	@Override
	public void listMyIssues(ListIssuesRequest request, StreamObserver<ListIssuesReponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Process Activity Requested is Null");
			}
			ListIssuesReponse.Builder entityValueList = IssueManagementLogic.listMyIssues(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * post: "/issue-management/issues"
	 */
	@Override
	public void createIssue(CreateIssueRequest request, StreamObserver<Issue> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Issue.Builder builder = IssueManagementLogic.createIssue(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * put: "/issue-management/issues/{id}"
	 */
	@Override
	public void updateIssue(UpdateIssueRequest request, StreamObserver<Issue> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Issue.Builder builder = IssueManagementLogic.updateIssue(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * delete: "/issue-management/issues/{id}"
	 */
	@Override
	public void deleteIssue(DeleteIssueRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Empty.Builder builder = IssueManagementLogic.deleteIssue(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/issue-management/issues/{issue_id}/comments"
	 */
	@Override
	public void listIssueComments(ListIssueCommentsRequest request, StreamObserver<ListIssueCommentsReponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListIssueCommentsReponse.Builder builder = IssueManagementLogic.listIssueComments(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * post: "/issue-management/issues/{issue_id}/comments"
	 */
	@Override
	public void createIssueComment(CreateIssueCommentRequest request, StreamObserver<IssueComment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			IssueComment.Builder builder = IssueManagementLogic.createIssueComment(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * put: "/issue-management/issues/{issue_id}/comments/{id}"
	 */
	@Override
	public void updateIssueComment(UpdateIssueCommentRequest request, StreamObserver<IssueComment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			IssueComment.Builder builder = IssueManagementLogic.updateIssueComment(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * delete: "/issue-management/issues/{issue_id}/comments/{id}"
	 */
	@Override
	public void deleteIssueComment(DeleteIssueCommentRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Empty.Builder builder = IssueManagementLogic.deleteIssueComment(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

}
